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
    const request = message.content.invocation_request
    const error = message.content.error
    if (error) {
      console.log('Failure to send message', message)
      return
    }
    const address = {
      type: request.reference.type,
      id: request.reference.key.stringKey
    }
    const addressString = `${address.type}-${address.id}`

    console.log(`received message ${addressString}: ${request.arguments}`)

    this.messages[addressString] = this.messages[addressString] || []
    this.messages[addressString].push({ timeStamp: moment(), message: request.arguments })

    this.addressables[addressString] || (this.addressables[addressString] = address)
  }

  async send(address, message) {
    await this.messagesService.send(address, message)

    return `Sent a message to ${address} on node ${this.messagesService.getNodeId()}: ${message}`
  }

  async getAddressables() {
    return {
      nodeId: this.messagesService.getNodeId(),
      addressables: Object.entries(this.addressables).map(([address, a]) => ({
        ...a,
        address,
        messageCount: this.messages[address] && this.messages[address].length
      }))
    }
  }

  async getMessages(id) {
    return {
      nodeId: this.messagesService.getNodeId(),
      messages: (id ? this.messages[id] : this.messages) || null
    }
  }

  getNodeId() {
    return { id: this.messagesService.getNodeId() }
  }
}

module.exports.default = MessagesController
