const grpc = require('grpc')
const protoLoader = require('@grpc/proto-loader')
const Path = require('path')
const { promisify } = require('util')
const moment = require('moment')

class MessagesController {

    messages = {}
    addressables = {}

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
        this.nodeManagement = grpc.loadPackageDefinition(nodeManagementDefinition).orbit.shared

        this.nodeManagementService = new this.nodeManagement.NodeManagement(url, grpc.credentials.createInsecure())
        this.nodeManagementService.JoinCluster = promisify(this.nodeManagementService.JoinCluster)
        this.nodeManagementService.RenewLease = promisify(this.nodeManagementService.RenewLease)

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
    }

    setLeaseFromResponse(lease) {
        return this.lease = {
            nodeId: lease.node_identity,
            challengeToken: lease.challenge_token,
            renewsAt: moment.unix(lease.renew_at.seconds),
            expiresAt: moment.unix(lease.expires_at.seconds)
        }
    }

    renewLeaseTimer(renewAt) {
        const timeout = renewAt.diff()
        console.log(`Next renewal: ${renewAt} in ${timeout} ms`)
        setTimeout(async () => {
            console.log(`renewing lease ${JSON.stringify(this.lease, null, 2)}`)
            const response = await this.nodeManagementService.RenewLease({
                challenge_token: this.lease.challengeToken,
                capabilities: {
                    addressableTypes: ["apiTest"]
                }
            }, await this.getMetadata())

            const lease = this.setLeaseFromResponse(response.lease_info)

            this.renewLeaseTimer(lease.renewsAt)
        }, timeout)
    }

    async joinCluster() {
        const response = await this.nodeManagementService.JoinCluster({
            capabilities: {
                addressableTypes: ["apiTest"]
            }
        })

        const lease = this.setLeaseFromResponse(response)

        this.renewLeaseTimer(lease.renewsAt)

        return lease
    }

    async getMetadata() {
        return new Promise(async (resolve, reject) => {
            if (!this.lease) {
                await this.joinCluster()
            }
            var meta = new grpc.Metadata();
            meta.add('nodeId', this.lease.nodeId);
            resolve(meta)
        })
    }

    async getConnection() {
        if (!this.messagesConnection) {
            const metadata = await this.getMetadata()
            this.messagesConnection = this.connectionService.Messages(metadata)
            this.messagesConnection.on('data', msg => {
                this.onReceive(msg)
            })

            this.messagesConnection.on('end', () => {
                console.log('end')
            })
        }
        return this.messagesConnection
    }

    async onReceive(message) {
        console.log('got a message', message)
        const address = {
            type: message.invocation_request.reference.type,
            id: message.invocation_request.reference.id
        }
        const addressString = `${address.type}-${address.id}`
        this.messages[addressString] = this.messages[addressString] || []
        this.messages[addressString].push({ timeStamp: moment(), message: message.invocation_request.value })

        this.addressables[addressString] || (this.addressables[addressString] = address)
    }

    async send(address, message) {
        const connection = await this.getConnection()

        await connection.write({
            invocation_request: {
                reference: {
                    type: "webtest",
                    id: address
                },
                value: message
            }
        })

        return `Sent a message to ${address} on node ${this.lease.nodeId}: ${message}`
    }

    async getAddressables() {
        return Object.values(this.addressables)
    }

    async getMessages(id) {
        return (id ? this.messages[id] : this.messages) || []
    }
}

module.exports.default = MessagesController
