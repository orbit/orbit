FROM golang:1.13.1

RUN GOPATH=~/go ; export GOPATH
RUN curl https://raw.githubusercontent.com/golang/dep/master/install.sh | sh
RUN git clone https://github.com/improbable-eng/grpc-web.git $GOPATH/src/github.com/improbable-eng/grpc-web
WORKDIR $GOPATH/src/github.com/improbable-eng/grpc-web
RUN dep ensure
RUN go install ./go/grpcwebproxy # installs into $GOPATH/bin/grpcwebproxy

CMD grpcwebproxy \
    --backend_addr=$BACKEND_HOST:$BACKEND_PORT \
    --backend_tls_noverify \
    --run_tls_server=false \
    --allow_all_origins
