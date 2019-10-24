import React, { useState } from 'react'
import './App.css'
import Client from './client'

import { Col, Row, Select } from 'antd'

const { Option } = Select

const nodes = process.env.REACT_APP_TEST_VALUE.split(",")

function App() {

    const [url, setUrl] = useState(nodes[0])

    return (
        <div className="app">
            <header className="app-header">
                <Row>
                    <Col span={16}>
                        <div className="app-header-label">Orbit Client Test</div>
                    </Col>
                    <Col span={8}>
                        <Select className="node-select" defaultValue={0} onChange={value => setUrl(nodes[value])}>
                            {nodes.map((node, index) => <Option value={index}>Node {index} ({node})</Option>)}
                        </Select>
                    </Col>
                </Row>
            </header>
            <Client url={url} />
        </div>
    );
}

export default App;
