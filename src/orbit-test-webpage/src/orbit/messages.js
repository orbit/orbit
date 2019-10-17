export default class Messages {

    constructor(url) {
        console.log(`connecting - ${url}`)

        this.url = url;
    }

    async getMessages(address) {
        return (await fetch(this.url + `/messages/${address}`)).json()
    }

    async getAddressables() {
        return (await fetch(this.url + '/addressables')).json()
    }

    async sendMessage(address, message) {
        console.log('send message', address, message)
        return await fetch(this.url + `/messages/${address}`,
            {
                method: 'POST',
                body: JSON.stringify({ message }),
                headers: {
                    "Content-Type": "application/json"
                }
            })
    }
}
