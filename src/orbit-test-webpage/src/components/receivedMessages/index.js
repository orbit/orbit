import React from 'react';
import { Timeline } from 'antd';
import './style.css'

export default function ReceivedMessages(props) {
  const { messages } = props;
  return (
    <div className="client">
      <Timeline className="incoming-messages">
        {messages.map(msg => <Timeline.Item>{msg}</Timeline.Item>)}
      </Timeline>

    </div>
  )
}
