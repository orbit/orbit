import React from 'react';
import { Timeline } from 'antd';
import './style.css'

export default function Client() {
  return (
    <div className="client">
      <Timeline className="incoming-messages">
        <Timeline.Item>first message</Timeline.Item>
        <Timeline.Item>second message</Timeline.Item>
      </Timeline>

    </div>
  );
}
