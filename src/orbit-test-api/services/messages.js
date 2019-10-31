const grpc = require('grpc')
const protoLoader = require('@grpc/proto-loader')
const Path = require('path')
const { promisify } = require('util')
const moment = require('moment')

class MessagesService {

    connectionRetry = 1000

    addressType = 'webtest'
    namespace = 'test'

    subscriptions = []
    leases = {}

    constructor(protoPath, url) {
        const sharedPath = Path.join(protoPath, 'orbit/shared')
        const nodeManagementDefinition = protoLoader.loadSync(
            Path.join(sharedPath, 'node_management.proto'),
            {
                keepCase: true,
                longs: String,
                enums: String,
                defaults: true,
                oneofs: true,
                includeDirs: [protoPath]
            })
        const NodeManagement = grpc.loadPackageDefinition(nodeManagementDefinition).orbit.shared.NodeManagement

        this.nodeManagementService = new NodeManagement(url, grpc.credentials.createInsecure())
        this.nodeManagementService.JoinCluster = promisify(this.nodeManagementService.JoinCluster)
        this.nodeManagementService.RenewLease = promisify(this.nodeManagementService.RenewLease)

        const addressableManagementDefinition = protoLoader.loadSync(
            Path.join(sharedPath, 'addressable_management.proto'),
            {
                keepCase: true,
                longs: String,
                enums: String,
                defaults: true,
                oneofs: true,
                includeDirs: [protoPath]
            })
        const AddressableManagement = grpc.loadPackageDefinition(addressableManagementDefinition).orbit.shared.AddressableManagement
        this.addressableManagementService = new AddressableManagement(url, grpc.credentials.createInsecure())
        this.addressableManagementService.RenewLease = promisify(this.addressableManagementService.RenewLease)

        const connectionDefinition = protoLoader.loadSync(
            Path.join(sharedPath, 'connection.proto'),
            {
                keepCase: true,
                longs: String,
                enums: String,
                defaults: true,
                oneofs: true,
                includeDirs: [protoPath]
            });
        this.connection = grpc.loadPackageDefinition(connectionDefinition).orbit.shared;
        this.connectionService = new this.connection.Connection(url, grpc.credentials.createInsecure())

        this.leaseTimer(moment())
    }

    getAddressKey(address) {
        return `${address.type}-${address.id}`
    }

    setNodeInfoFromResponse(node) {
        return this.node = {
            nodeId: node.id,
            nodeKey: node.id.key,
            namespace: node.id.namespace,
            challengeToken: node.lease.challenge_token,
            renewsAt: moment.unix(node.lease.renew_at.seconds),
            expiresAt: moment.unix(node.lease.expires_at.seconds)
        }
    }

    async leaseTimer() {
        if (!this.node) {
            await this.joinCluster()
        }

        if (this.node && !this.messagesConnection) {
            await this.openConnection()
        }

        setTimeout(async () => {
            if (this.node) {
                await this.renewNodeLease()
            }
            this.leaseTimer()
        }, this.node ? this.node.renewsAt.diff() : this.connectionRetry)
    }

    async joinCluster(namespace = this.namespace) {
        var meta = new grpc.Metadata()
        meta.add('namespace', namespace)

        try {
            const response = await this.nodeManagementService.JoinCluster({
                capabilities: {
                    addressableTypes: [this.addressType]
                }
            }, meta)
            console.log('join cluster response', JSON.stringify(response, null, 2))

            this.setNodeInfoFromResponse(response.info)
        }
        catch (e) {
            console.log('Failed to join cluster')
        }
    }

    async openConnection() {
        if (!this.messagesConnection && this.node) {
            const metadata = this.getMetadata()
            try {
                console.log('connecting to service')
                this.messagesConnection = this.connectionService.openStream(metadata)
                console.log('connected to service')
            }
            catch (e) {
                console.log('failed to connect to messaging service', e)
                return
            }
            this.messagesConnection.on('data', msg => {
                this.onReceive(msg)
            })

            this.messagesConnection.on('end', () => {
                console.log('connection closed')
                this.messagesConnection.end()
                this.messagesConnection = null
            })
        }

        return this.messagesConnection
    }

    async renewNodeLease() {
        const response = await this.nodeManagementService.RenewLease({
            challenge_token: this.node.challengeToken,
            capabilities: {
                addressableTypes: [this.addressType]
            }
        }, await this.getMetadata())
        return this.setNodeInfoFromResponse(response.info)
    }

    async getOrRenewAddressableLease(address) {
        const addressKey = this.getAddressKey(address)
        var lease = this.leases[addressKey]
        if (!lease || moment().isAfter(lease.renewAt)) {
            lease = this.renewAddressableLease(address)
        }

        return lease
    }

    async renewAddressableLease(address) {
        const response = await this.addressableManagementService.RenewLease({
            reference: address
        }, await this.getMetadata())

        if (response.status != 'OK') {
            console.log(`Failed to renew addressable ${JSON.stringify(address)}: ${response.status}`, response.error_description)
        }
        else {
            const lease = {
                nodeId: response.lease.nodeId,
                reference: { type: response.lease.reference.type, key: response.lease.reference.key.stringKey },
                renewAt: moment.unix(response.lease.renew_at.seconds),
                expiresAt: moment.unix(response.lease.expires_at.seconds)
            }

            this.leases[this.getAddressKey(address)] = lease
            return lease
        }
    }

    getMetadata() {
        var meta = new grpc.Metadata()
        meta.add('nodeKey', this.node.nodeKey)
        meta.add('namespace', this.node.namespace)
        return meta
    }

    async getConnection() {
        if (!this.messagesConnection) {
            await this.openConnection()
        }
        return this.messagesConnection
    }

    subscribe(callback) {
        this.subscriptions.push(callback)
    }

    async onReceive(message) {
        Promise.all(this.subscriptions.map(async callback => {
            try {
                return callback(message)
            }
            catch { }
        }))
    }

    async send(address, message) {
        console.log('sending message to', address, message)
        const connection = await this.getConnection()
        await connection.write({
            content: {
                invocation_request: {
                    reference: {
                        type: this.addressType,
                        key: { stringKey: address }
                    },
                    method: 'showMessage',
                    arguments: message
                }
            }
        })
    }

    getNodeId() {
        return this.node && this.node.nodeKey || ''
    }
}

module.exports.default = MessagesService