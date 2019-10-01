FROM node:12

ENV TARGETDIR /opt/orbit/web
ENV NODE_PATH=$TARGETDIR
ENV ASSETS_PATH=/public
ENV APP_PORT=3000

EXPOSE $APP_PORT

COPY ./src/orbit-test-webpage/ $TARGETDIR/
# COPY ./src/orbit-test-webpage/package.json $TARGETDIR/package.json
WORKDIR $TARGETDIR

RUN yarn
ADD ./src/orbit-test-webpage/ $TARGETDIR

CMD npm run dev
