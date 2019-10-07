const grpc = require('grpc')
const protoLoader = require('@grpc/proto-loader')
const Path = require('path')
const { promisify } = require('util')

class MessagesController {

    messages = {}

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

    async joinCluster() {
        const response = await this.nodeManagementService.JoinCluster({
            capabilities: {
                addressableTypes: ["apiTest"]
            }
        })

        return {
            nodeId: response.node_identity,
            challengeToken: response.challengeToken,
            renewsAt: response.renew_at,
            expiresAt: response.expires_at
        }
    }

    async getMetadata() {
        return new Promise(async (resolve, reject) => {
            if (!this.lease) {
                this.lease = await this.joinCluster()
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
                console.log('got a message', msg)
            })

            this.messagesConnection.on('end', () => {
                console.log('end')
            })
        }
        return this.messagesConnection
    }

    async send(address, message) {
        const connection = await this.getConnection()

        connection.write({
            InvocationRequest: {
                reference: {
                    type: "webtest",
                    id: address
                },
                value: message
            }
        })

        return `Sent a message to ${address} on node ${this.lease.nodeId}: ${message}`
    }
}

module.exports.default = MessagesController
