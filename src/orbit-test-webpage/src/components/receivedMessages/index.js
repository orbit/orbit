import React from 'react';
import { Timeline } from 'antd';
import moment from 'moment';
import './style.css'

export default function ReceivedMessages(props) {
  const { messages } = props;
  return (
    <section className="incoming-messages">
      <h3>Received Messages</h3>
      <Timeline>
        {(messages || []).map(msg => <Timeline.Item>{moment(msg.timeStamp).format('lll')} - {msg.message}</Timeline.Item>)}
      </Timeline>
    </section>
  )
}
