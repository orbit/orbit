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
      addressables: { }
    }

    this.loadAddressables()
  }

  loadAddressables() {
    setTimeout(() => {
      this.sender.getMessages().then(addressables => {
        this.setState({ addressables })
      })
      this.loadAddressables()
    }, 2000)
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
            <Addressables addressables={Object.keys(this.state.addressables)} select={address => this.setState({ currentAddressable: address })} />
          </Col>
          <Col span={12}>
            <ReceivedMessages messages={this.state.currentAddressable && this.state.addressables[this.state.currentAddressable]} />
          </Col>
        </Row>
      </div>
    )
  }
}
