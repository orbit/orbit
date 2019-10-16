import React, { useState } from 'react'
import './App.css'
import Client from './client'

import { Row, Col, Select } from 'antd'
const { Option } = Select

const nodes = [
  "http://localhost:8080",
  "http://localhost:8081"
]

function App() {

  const [url, setUrl] = useState(nodes[0])

  return (
    <div className="app">
      <header className="app-header">
        <Row>
          <Col span={20}>
            <div className="app-header-label">Orbit Client Test</div>
          </Col>
          <Col span={4}>
            <Select className="node-select" defaultValue={0} onChange={value => setUrl(nodes[value])}>
              <Option value={0}>Orbit Node 1</Option>
              <Option value={1}>Orbit Node 2</Option>
            </Select>
          </Col>
        </Row>
      </header>
      <Client url={url} />
    </div>
  );
}

export default App;
