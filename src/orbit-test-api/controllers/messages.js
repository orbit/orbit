const grpc = require('grpc')
const protoLoader = require('@grpc/proto-loader')
const Path = require('path')
const { promisify } = require('util')

class MessagesController {
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
        const creds = grpc.credentials.createInsecure()
        console.log('creds', creds)
        this.nodeManagementService = new this.nodeManagement.NodeManagement(url, creds)
        this.nodeManagementService.JoinCluster = promisify(this.nodeManagementService.JoinCluster)


        const addressableDefinition = protoLoader.loadSync(
            Path.join(sharedPath, 'addressable.proto'),
            {
                keepCase: true,
                longs: String,
                enums: String,
                defaults: true,
                oneofs: true,
                includeDirs: [protoPath]
            });
        const messageDefinition = protoLoader.loadSync(
            Path.join(sharedPath, 'messages.proto'),
            {
                keepCase: true,
                longs: String,
                enums: String,
                defaults: true,
                oneofs: true,
                includeDirs: [protoPath]
            });
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
        this.connectionService = new this.connection.Connection(url, creds)

    }

    async connect() {
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
            if (!this.metadata) {
                this.metadata = await this.connect()
            }
            var meta = new grpc.Metadata();
            meta.add('nodeId', this.metadata.nodeId);
            resolve(meta)

            
            // if (!this.metadata) {
            //     this.metadata =  await this.connect()
            //     var meta = new grpc.Metadata();
            //     meta.add('nodeId', this.metadata.nodeId);
            //     resolve(meta)
            // }
            // else {
            //     var meta = new grpc.Metadata();
            //     meta.add('nodeId', this.metadata.nodeId);
            //     resolve(meta)
            // }
            // if (this.metadata) {
            //     var meta = new grpc.Metadata();
            //     meta.add('nodeId', this.metadata.nodeId);
            //     resolve(meta)
            // }
            // else {
            //     this.connect().then(result => {
            //         this.metadata = result
            //         var meta = new grpc.Metadata();
            //         meta.add('nodeId', this.metadata.nodeId);
            //         resolve(meta)
            //         })
            // }
        })
    }

    async send(address, message) {
        const metadata = await this.getMetadata()
        console.log(metadata)
        const call = this.connectionService.Messages()

        // call.on('data', function (msg) {
        //     console.log('got a message', msg)
        // })

        call.write({
            InvocationRequest: {
                reference: {
                    type: "webtest",
                    id: address
                },
                value: message
            }
        })

        // console.log('call', call)
        return `Sent a message to ${address} on node ${metadata.nodeId}: ${message}`
    }
}

module.exports.default = MessagesController
