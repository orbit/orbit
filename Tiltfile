k8s_yaml(helm(
    './charts/orbit',
    name='orbit',
    namespace="orbit"
))

docker_build('orbitframework/orbit',
    '.',
    dockerfile='./docker/server/Dockerfile',
    only=[
        'src/orbit-application/build/libs',
        'docker'
    ],
    live_update=[sync('src/orbit-application/build/libs', '/opt/orbit/libs')]
)

k8s_resource('orbit-server',
    port_forwards=['50056:50056', '5005:5005', '8080:8080']
)
