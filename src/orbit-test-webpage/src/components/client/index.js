import React, { Component } from 'react';
import './style.css'
import { Row, Col, Input, Button } from 'antd';
import ReceivedMessages from '../receivedMessages'

import Messages from '../../orbit/messages'

export default class Client extends Component {
  sender;
  host;
  port;
  address;
  message;

  connect() {
    this.sender = new Messages(this.host, this.port)
  }

  sendMessage() {
    this.sender.sendMessage(this.address, this.message)
  }

  render() {
    return (
      <div className="client">
        <section className="messageEntry">
          <Row>
            <Col span={12}><Input placeholder="host" onChange={host => this.host = host.target.value} /></Col>
            <Col span={8}><Input placeholder="port" onChange={port => this.port = port.target.value} /></Col>
            <Col span={4}><Button onClick={() => this.connect()}>Connect</Button></Col>
          </Row>
          <Input placeholder="Addressable" />
          <Input.TextArea placeholder="Message" ref={msg => this.message = msg} />
          <Button onClick={() => this.sendMessage()}>Send</Button>
        </section>
        <ReceivedMessages messages={['first message', 'second message']} />
      </div>
    )
  }
}
