import { promisify } from 'es6-promisify'

export default class Messages {

    constructor(host = 'localhost', port = 8080) {
        console.log(`connecting - http://${host}:${port}`)

        this.url = `http://${host}:${port}`;
    }

    async getMessages(address) {
        return (await fetch(this.url + '/messages')).json()
    }

    async sendMessage(address, message) {


    }
}
