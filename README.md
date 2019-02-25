# kees - Kubernetes External Environment Secrets

[![Build Status](https://travis-ci.org/bsycorp/kees.svg?branch=master)](https://travis-ci.org/bsycorp/kees)

https://hub.docker.com/r/bsycorp/kees-init/

In our current draft of Kubernetes Secret Management, we have three components: Init Container, Creator and Exporter.

The functionalities of each component are outlined as following:

## Creator:
* Watches for relevant pods
* Read secret annotations
* Create secrets that do not exist yet

## Init Container:
* Read annotations of requested secrets and resources
* Retrieve secret from Parameter Store
* Write secrets to EmptyDir for app container to consume

## Exporter:
* Is called during build/packaging stage, not a runtime component
* Generates a terraform IAM policy that matches the secret/resource annotations in the manifest to maintain a single source of truth
* IAM policy changes will be reviewed as part of code review (in the manifest) and reviewed again during terraform plan

## How does Init Container work?

Init container are used to initialise a pod before an application container runs. In our case, it will read the secrets from Parameter Store, and make it available to the app container before it spins up.


### Step 1:

Before an application is run, the build/package step adds an init container to all relevant pods as part of the `gradle build` task.

```yaml
spec:
  containers:
  ...
  initContainers:
  - name: init-container
    image: bsycorp/kees/init:latest
    volumeMounts:
    - mountPath: /secret
      name: secret-volume
    - mountPath: /annotations
      name: annotations
```

During the package stage we also generate a terraform IAM policy. The application needs to be given some permissions to the secret storage (SSM / Parameter Store) this is done via the Exporter.
We want the manifest annotations to be the source of truth for defining what secrets an application needs access too, but this needs to be enforced by IAM, so
the exporter is run at build/package time to generate a policy that ensures the two are aligned.

### Step 2:

Then it will use the downwardAPI to read annotations from the pod spec. 

```yaml
volumes:
- name: secret-volume
  emptyDir: {}
- name: annotations
  downwardAPI:
    items:
      - path: "annotations"
        fieldRef:
          fieldPath: metadata.annotations
```

This will load the annotations into a file for init container to consume.

```yaml
spec:
  template:
    metadata:
      annotations:
        iam.amazonaws.com/role: cluster-app-role
        secret.bsycorp.com/signingKey.v1_public: "kind=DYNAMIC,type=RSA,size=2048,foo=bar"
        secret.bsycorp.com/signingKey.v1_private: "kind=DYNAMIC,type=RSA,size=2048,foo=bar"
        secret.bsycorp.com/db.password: "kind=REFERENCE,type=PASSWORD"
```

The above spec will be read into /annotations/annotations as following:
```
secret.bsycorp.com/signingKey.v1_public="kind=DYNAMIC,type=RSA,size=2048,foo=bar"
secret.bsycorp.com/signingKey.v1_private="kind=DYNAMIC,type=RSA,size=2048,foo=bar"
secret.bsycorp.com/db.password="kind=REFERENCE,type=PASSWORD"
```

### Step 3:

The init image will then

* Process the annotations and extract secret keys
* Query Parameter Store to retrieve the secret values (as restricted by its IAM role)
* Save the secret key value pairs to a file in EmptyDir

An example output /secret/secret would be as following:

```
db.password=fakepassword
signingKey.v1_private=privatekeydummyvalue
signingKey.v1_public=publickeydummyvalue
```

### Step 4:

When the init container completes, app container will spin up, and will be able to consume the secrets from /secret/secret in the EmptyDir volume.

## GPG support
GPG key generation requires the userId to be provided as an annotation parameter. This must be of the form of "user<email>".
A random password is generated and used for the GPG key pair generation. The output is an encoded armored GPG key pair and corresponding password.

For example:
```
secret.bsycorp.com/gpg.v1_public: "kind=DYNAMIC,type=GPG,size=2048,userId=foo<bar@email.com>"
secret.bsycorp.com/gpg.v1_private: "kind=DYNAMIC,type=GPG,size=2048,userId=foo<bar@email.com>"
secret.bsycorp.com/gpg.v1_password: "kind=DYNAMIC,type=GPG,size=2048,userId=foo<bar@email.com>"
```

Alternatively the userId could be provided as a custom annotation. This enables the userId to be available as a secret in the container.

For example:
```
custom.bsycorp.com/gpg.v1_userId: "fixedValue=foo<bar@email.com>"
secret.bsycorp.com/gpg.v1_public: "kind=DYNAMIC,type=GPG,size=2048"
secret.bsycorp.com/gpg.v1_private: "kind=DYNAMIC,type=GPG,size=2048"
secret.bsycorp.com/gpg.v1_password: "kind=DYNAMIC,type=GPG,size=2048"
```

Note: The deterministic provider will generate deterministic RSA keys and use those keys for the GPG secret key creation.
Although the armored output of these keys will be different every time, the underlying RSA keys are the same and will be able to encrypt/decrypt/sign.

# Reference

* Init containers: https://kubernetes.io/docs/concepts/workloads/pods/init-containers/