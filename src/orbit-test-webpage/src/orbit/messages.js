import protobuf from 'protobufjs'

export default class Messages {

    constructor(host, port = 80) {
        this.host = host
        this.port = port

        protobuf.load('orbit/shared/messages.proto', (err, root) => {
            console.log(root)
            this.Message = root.lookupType("orbit.shared.Message");

            console.log('msgs', this.Message)
        })
    }

    async sendMessage(address, message) {

        const payload = this.Message.create({
            InvocationRequest: {
                reference: {
                    type: "webtest",
                    id: address
                },
                value: message
            }
        })


        console.log("payload created", payload);

        const response = await fetch(`http://${this.host}:${this.port}`, {
            method: 'POST',
            data: this.Message.encode(payload).finish(),
            headers: {
                'Content-Type': 'application/x-protobuf'
            }
        });

        console.log('response from post', response)

    }
}
