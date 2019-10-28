import React from 'react'
import { Provider as ReduxProvider } from "react-redux";
import configureStore from "../modules/store";

import './App.css'
import Client from './client'

import { Col, Row } from 'antd'

const url = process.env.REACT_APP_API_URL
const store = configureStore({});

function App() {

    return (
        <ReduxProvider store={store}>
            <div className="app">
                <header className="app-header">
                    <Row>
                        <Col span={16}>
                            <div className="app-header-label">Orbit Client Test</div>
                        </Col>
                    </Row>
                </header>
                <Client url={url} />
            </div>
        </ReduxProvider>
    );
}

export default App;
