const moment = require('moment')

class MessagesController {

  addressType = 'webtest'
  namespace = 'test'

  messages = {}
  addressables = {}

  constructor(messagesService) {
    this.messagesService = messagesService

    this.messagesService.subscribe(message => this.onReceive(message))
  }

  async onReceive(message) {
    console.log('got a message', message)
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
    await this.messagesService.send(address, message)

    return `Sent a message to ${address} on node ${this.messagesService.getNodeId()}: ${message}`
  }

  async getAddressables() {
    return {
      nodeId: this.messagesService.getNodeId(),
      addressables: Object.entries(this.addressables).map(([id, a]) => ({
        ...a,
        id,
        messageCount: this.messages[id] && this.messages[id].length
      }))
    }
  }

  async getMessages(id) {
    return {
      messages: (id ? this.messages[id] : this.messages) || null
    }
  }

  getNodeId() {
    return { id: this.messagesService.getNodeId() }
  }
}

module.exports.default = MessagesController
