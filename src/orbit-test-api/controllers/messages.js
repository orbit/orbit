const moment = require('moment')

class MessagesController {

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

    const leaseActive = await this.getOrRewewAddressableLease(request.reference)

    if (!leaseActive) {
      console.log('Received message on unleased addressable', request.reference)
      return
    }
    const address = {
      type: request.reference.type,
      id: request.reference.key.stringKey
    }
    const addressKey = this.messagesService.getAddressKey(address)

    console.log(`received message ${addressKey}: ${request.arguments}`)

    this.messages[addressKey] = this.messages[addressKey] || []
    this.messages[addressKey].push({ timeStamp: moment(), message: request.arguments })

    this.addressables[addressKey] || (this.addressables[addressKey] = address)
  }

  async send(address, message) {
    await this.messagesService.send(address, message)

    return `Sent a message to ${address} on node ${this.messagesService.getNodeId()}: ${message}`
  }

  async getOrRewewAddressableLease(address) {
    const lease = await this.messagesService.getOrRenewAddressableLease(address)
    return (lease && lease.nodeId.key == this.messagesService.getNodeId())
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
