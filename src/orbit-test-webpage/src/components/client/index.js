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
  sender;

  sendMessage() {
    this.sender.sendMessage(this.address, this.message)
  }

  constructor(props) {
    super(props)

    this.state = {
      addressables: [],
      messages: []
    }

    this.sender = new Messages(props.url)

    this.refreshLoop()
  }

  refreshLoop() {
    this.refresh()
    setTimeout(() => {
      this.refreshLoop()
    }, 5000)
  }

  refresh() {
    this.sender.getMessages(this.state.currentAddressable).then(messages => {
      this.setState({ messages })
    })
    this.sender.getAddressables().then(addressables => {
      this.setState({ addressables })
    })
  }

  changeCurrentAddressable(address) {
    this.setState({ currentAddressable: address })
    this.refresh()
  }

  componentDidUpdate() {
    if (this.sender.url !== this.props.url) {
      this.sender.url = this.props.url
      this.refresh()
    }
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
