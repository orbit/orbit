const Router = require('koa-router')
const router = new Router()
const MessagesController = require('../controllers/messages').default
const messagesController = new MessagesController('./proto', process.env.ORBIT_URL)

messagesController.joinCluster()

router.get('post', async ctx => ctx.ok(await messagesController.send(ctx.request.query.address, ctx.request.query.message)))
router.post('messages/:id', async ctx => ctx.ok(await messagesController.send(ctx.params.id, ctx.request.body.message)))
router.get('messages/:id?', async ctx => {
    const messages = await messagesController.getMessages(ctx.params.id)
    return messages ? ctx.ok(messages) : ctx.notFound()
})
router.get('addressables', async ctx => ctx.ok(await messagesController.getAddressables()))
router.get('node', async ctx => ctx.ok(messagesController.getNodeId()))

module.exports = router.routes()
