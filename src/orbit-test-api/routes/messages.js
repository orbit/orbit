const Router = require('koa-router')
const router = new Router()
const MessagesController = require('../controllers/messages').default
const messagesController = new MessagesController('./proto', process.env.ORBIT_URL)

router.get('post', async ctx => ctx.ok(await messagesController.send(ctx.request.query.address, ctx.request.query.message)))
router.get('messages/:id?', async ctx => ctx.ok(await messagesController.getMessages(ctx.params.id)))
router.get('addressables', async ctx => ctx.ok(await messagesController.getAddressables()))

module.exports = router.routes()
