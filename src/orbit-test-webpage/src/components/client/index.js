import React, { Component } from 'react'
import './style.css'
import { Button, Col, Form, Input, Row } from 'antd'
import ReceivedMessages from '../receivedMessages'
import Addressables from '../addressables'
import { connect } from "react-redux"
import { reportMessages, reportAddressables } from '../../actions/messages'

import Messages from '../../orbit/messages'

const mapStateToProps = state => ({
    addressables: Object.values(state.messages.addressables),
    messages: state.messages.messages
});

const mapDispatchToProps = dispatch => ({
    reportMessages: (addressableId, messages) => dispatch(reportMessages(addressableId, messages)),
    reportAddressables: (nodeid, messages) => dispatch(reportAddressables(nodeid, messages)),
});

class Client extends Component {
    sender;
    host;
    port;
    address;
    message;
    sender;

    sendMessage(e) {
        e.preventDefault()
        const { form } = this.props

        this.sender.sendMessage(form.getFieldValue("address"), form.getFieldValue("message"))
    }

    constructor(props) {
        super(props)

        this.state = {
            addressables: [],
            messages: {}
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

    refresh(address) {
        const current = address || this.state.currentAddressable
        if (current) {
            this.sender.getMessages(current).then(messages => {
                this.props.reportMessages(current, messages)
            }).catch(() => {})
        }
        this.sender.getAddressables().then(result => {
            this.props.reportAddressables(result.nodeId, result.addressables)
        })
    }

    changeCurrentAddressable(address) {
        this.setState({ currentAddressable: address })
        this.refresh(address)
    }

    componentDidUpdate() {
        if (this.sender.url !== this.props.url) {
            this.sender.url = this.props.url
            this.refresh()
        }
    }

    render() {
        const { getFieldDecorator } = this.props.form;

        const formItemLayout = {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 8 },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 16 },
            }
        }
        return (
            <div className="client">
                <Row>
                    <Col span={12}>
                        <section className="messageEntry">
                            <Form {...formItemLayout} onSubmit={e => this.sendMessage(e)}>
                                <Form.Item label="Address">
                                    {getFieldDecorator('address')(
                                        <Input placeholder="Address" ref={address => this.address = address} />)}
                                </Form.Item>
                                <Form.Item label="Message">
                                    {getFieldDecorator('message')(
                                        <Input.TextArea placeholder="Message" ref={msg => this.message = msg} />)}
                                </Form.Item>
                                <Form.Item>
                                    <Button type="primary" htmlType="submit">Send</Button>
                                </Form.Item>
                            </Form>
                        </section>
                    </Col>
                </Row>
                <Row gutter={6}>
                    <Col span={12}>
                        <Addressables addressables={this.props.addressables}
                            select={address => this.changeCurrentAddressable(address)} />
                    </Col>
                    <Col span={12}>
                        <ReceivedMessages messages={this.props.messages[this.state.currentAddressable]} />
                    </Col>
                </Row>
            </div>
        )
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Form.create()(Client))
