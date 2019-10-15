import React, { Component } from 'react';
import './style.css'
import { Row, Col, Input, Button } from 'antd';
import ReceivedMessages from '../receivedMessages'
import Addressables from '../addressables'

import Messages from '../../orbit/messages'

export default class Client extends Component {
  sender;
  host;
  port;
  address;
  message;

  sender = new Messages()

  sendMessage() {
    this.sender.sendMessage(this.address, this.message)
  }

  constructor() {
    super()

    this.state = {
      addressables: [],
      messages: []
    }

    this.loadAddressables()
  }

  loadAddressables() {
    setTimeout(() => {
      this.sender.getAddressables().then(addressables => {
        this.setState({ addressables })
      })
      this.loadAddressables()
    }, 2000)
  }

  changeCurrentAddressable(address) {
    this.setState({ currentAddressable: address })

    this.sender.getMessages(address).then(messages => {
      this.setState({ messages })
    })
  }

  render() {
    return (
      <div className="client">
        <Row>
          <Col span={12}>
            <section className="messageEntry">
              <Input placeholder="Addressable" />
              <Input.TextArea placeholder="Message" ref={msg => this.message = msg} />
              <Button onClick={() => this.sendMessage()}>Send</Button>
            </section>
          </Col>
        </Row>
        <Row gutter={6}>
          <Col span={12}>
            <Addressables addressables={this.state.addressables} select={address => this.changeCurrentAddressable(address)} />
          </Col>
          <Col span={12}>
            <ReceivedMessages messages={this.state.messages} />
          </Col>
        </Row>
      </div>
    )
  }
}
