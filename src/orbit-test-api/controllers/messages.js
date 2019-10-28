const grpc = require('grpc')
const protoLoader = require('@grpc/proto-loader')
const Path = require('path')
const { promisify } = require('util')
const moment = require('moment')

class MessagesController {

  addressType = 'webtest'
  namespace = 'test'

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

    this.joinCluster(this.namespace)
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

  renewLeaseTimer(renewAt) {
    const timeout = renewAt.diff()
    console.log(`Next renewal: ${renewAt} in ${timeout} ms`)
    setTimeout(async () => {
      const response = await this.nodeManagementService.RenewLease({
        challenge_token: this.node.challengeToken,
        capabilities: {
          addressableTypes: [this.addressType]
        }
      }, await this.getMetadata())

      const lease = this.setNodeInfoFromResponse(response.info)

      this.renewLeaseTimer(lease.renewsAt)
    }, timeout)
  }

  async joinCluster(namespace) {
    var meta = new grpc.Metadata()
    meta.add('namespace', namespace)

    const response = await this.nodeManagementService.JoinCluster({
      capabilities: {
        addressableTypes: [this.addressType]
      }
    }, meta)
    console.log('join cluster response', JSON.stringify(response, null, 2))

    const lease = this.setNodeInfoFromResponse(response.info)
    this.renewLeaseTimer(lease.renewsAt)
    return lease
  }

  getMetadata() {
      var meta = new grpc.Metadata()
      meta.add('nodeKey', this.node.nodeKey)
      meta.add('namespace', this.node.namespace)
      return meta
  }

  async getConnection() {
    if (!this.messagesConnection) {
      const metadata = this.getMetadata()
      this.messagesConnection = this.connectionService.openStream(metadata)
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
    console.log('got a message', JSON.stringify(message, null, 2))
    const address = {
      type: message.content.invocation_request.reference.type,
      id: message.content.invocation_request.reference.key.stringKey
    }
    const addressString = `${address.type}-${address.id}`
    this.messages[addressString] = this.messages[addressString] || []
    this.messages[addressString].push({ timeStamp: moment(), message: message.content.invocation_request.arguments })

    this.addressables[addressString] || (this.addressables[addressString] = address)
  }

  async send(address, message) {
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

    return `Sent a message to ${address} on node ${this.node.nodeKey}: ${message}`
  }

  async getAddressables() {
    return {
      nodeId: this.node && this.node.nodeKey,
      addressables: Object.entries(this.addressables).map(([id, a]) => ({
        ...a,
        id,
        messageCount: this.messages[id] && this.messages[id].length
      }))
    }
  }

  async getMessages(id) {
    return (id ? this.messages[id] : this.messages) || null
  }

  getNodeId() {
    return { id: this.node && this.node.nodeKey || '' }
  }
}

module.exports.default = MessagesController
