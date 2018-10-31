# bnd in docker

## build
```
git clone https://github.com/bndtools/bnd.git
cd bnd
cd docker
./build.sh
```

### set an alias
```
alias bnd-docker='docker run -it -v $HOME:$HOME -v $(pwd):/data bndtoolsorg/bnd:latest'
```

### use bnd
`bnd-docker --help` or `bnd-docker shell`

### cleanup, remove bnd/docker/tmp
`rm -r tmp/`
