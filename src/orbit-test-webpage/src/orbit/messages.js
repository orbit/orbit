import { Message } from '../proto/orbit/shared/messages_pb'
// import protobuf from 'protobufjs'
import { ConnectionClient } from '../proto/orbit/shared/connection_grpc_web_pb.js'
import { JoinClusterRequest, NodeCapabilities, NodeManagementClient } from '../proto/orbit/shared/node_management_grpc_web_pb.js'

import { promisify } from 'es6-promisify'

export default class Messages {

    constructor(host = 'localhost', port = 50056) {
        console.log(`connecting - http://${host}:${port}`)
        this.messageService = new ConnectionClient(`http://${host}:${port}`);
        this.nodeManagementService = new NodeManagementClient(`http://${host}:${port}`);

    console.log('this.messageService', this.messageService)
        this.messageService.messages = promisify(this.messageService.messages)
        this.nodeManagementService.joinCluster = promisify(this.nodeManagementService.joinCluster)

        this.host = host
        this.port = port

        // protobuf.load('orbit/shared/messages.proto', (err, root) => {
        //     console.log(root)
        //     this.Message = root.lookupType("orbit.shared.Message");

        //     console.log('msgs', this.Message)
        // })
    }

    async connect() {

        // const call = this.nodeManagementService.joinCluster(request, {}, (err, response) => {
        //     if (err) {
        //         console.log(err.code);
        //         console.log(err.message);
        //     } else {
        //         console.log(response.getNodeIdentity());
        //     }
        // })
        console.log('connecting')

        const request = new JoinClusterRequest(
            new NodeCapabilities(["web place"])
        )

        const response = await this.nodeManagementService.joinCluster(request, {})

        console.log('connected', response.getNodeIdentity())

    }

    async sendMessage(address, message) {
        const msgResponse = await this.messageService.messages()

        console.log('Msg response', msgResponse)


        // call.on('status', status => {
        //     console.dir(status.code)
        //     console.dir(status.details)

        // })

        // const msgResponse = this.messageService.messages()

        // console.dir(msgResponse)

        // this.messageService.message(address, {
        //     InvocationRequest: {
        //         reference: {
        //             type: "webtest",
        //             id: address
        //         },
        //         value: message
        //     }
        // })

        // console.log("payload created", payload);

        // var request = new proto.Message();
        // request.setMessage({
        //     InvocationRequest: {
        //         reference: {
        //             type: "webtest",
        //             id: address
        //         },
        //         value: message
        //     }
        // });
        // const response = await fetch(`http://${this.host}:${this.port}`, {
        //     method: 'POST',
        //     data: this.Message.encode(payload).finish(),
        //     headers: {
        //         'Content-Type': 'application/x-protobuf'
        //     }
        // });

        // console.log('response from post', response)

    }
}
