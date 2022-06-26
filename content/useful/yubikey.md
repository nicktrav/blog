Yubikeys
========

 key  | value   
 ---  | ---      
name  | yubikeys

Commands for setting up a Yubikey, with GPG key and SSH key using (PIV).

Relies on the following:

- [`ykman`](https://developers.yubico.com/yubikey-manager/)
- [`gnupg`](https://gnupg.org/)

## GPG

Set up roughly following
[this](https://florin.myip.org/blog/easy-multifactor-authentication-ssh-using-yubikey-neo-tokens)
guide.

```bash
# Insert key ... duh

# Bring up gpg
$ gpg --card-edit

# Enter admin mode
gpg/card> admin

# Set the PIN and admin PIN
gpg/card> passwd

# Generate a key
gpg/card> generate
```

Set up the agent:

```bash
pinentry-program $PATH_TO/pinentry-mac
default-cache-ttl 600
max-cache-ttl 7200
```

Add the following to your profile:

```bash
GPG_TTY="$(tty)"
export GPG_TTY
gpgconf --launch gpg-agent
```

### Seeding local instance

Import keys in RAM (e.g. `/dev/shm`).

```bash
$ gpg --import private.key
```

Export subkeys.

```bash
$ gpg --armor --output subkeys.key --export-secret-subkeys $KEY
```

Remove all secret keys.

```bash
$ gpg --delete-secret-keys $KEY
```

Re-import subkey secret keys.

```bash
$ gpg --import subkeys.key
```

Update the subkey to point at the Yubikey.

```bash
$ gpg --expert --edit-key $KEY

gpg> key $NUM
gpg> keytocard
```

Select signature key. Enter the passphrases to unlock the key, and then enter
the Yubikey admin PIN (`12345678`).

Save and exit.

List the secret keys, which should show a pointer to the card for the subkey.

```bash
$ gpg --list-secret-keys
------------------------------
sec>  ... snip ...
ssb>  ... snip ...
```

Shred any remaining key material.

```bash
$ shred -u $FILE [$FILE ...]
```

## SSH

The following generates a new SSH keypair that resides on the key. Adapted from
the following:

- [Using PIV for SSH through PKCS #11
  (Official)](https://developers.yubico.com/PIV/Guides/SSH_with_PIV_and_PKCS11.html)
- [Using a Yubikey to Secure SSH on
  macOS](https://blog.snapdragon.cc/2019/04/27/using-a-yubikey-to-secure-ssh-on-macos/)
- [Authenticating SSH with PIV and PKCS#11
  (client)](https://ruimarinho.gitbooks.io/yubikey-handbook/content/ssh/authenticating-ssh-with-piv-and-pkcs11-client/)

Ensure that
[`yubico-piv-tool`](https://developers.yubico.com/yubico-piv-tool/Releases/) is
installed.

Ensure [`opensc`](https://github.com/OpenSC/OpenSC/) is installed.

Create a volume in memory to use for temporarily storing the key material (see
[`ramdisk`](./ramdisk.md)).

```bash
# Switch into the ramdisk
$ cd /volumes/secure
$ umask 077

# Create a public key.
# Enter the management key when prompted.
# Default: 010203040506070801020304050607080102030405060708
$ yubico-piv-tool \
  -a generate \
  -s 9a \
  -k \
  --pin-policy=once \
  --touch-policy=always \
  --algorithm=ECCP256 \
  -o public.pem


# Create a self-signed certificate
# Enter the PIN when prompted, then tap the key.
$ yubico-piv-tool \
  -a verify-pin \
  -a selfsign-certificate \
  -s 9a \
  -S '/CN=ssh/' \
  --valid-days=365 \
  -i public.pem \
  -o cert.pem

# Import the certificate
# Enter the management key when prompted.
$ yubico-piv-tool -k -a import-certificate -s 9a -i cert.pem
```

Confirm that they key has been added:

```bash
$ yubico-piv-tool -a status
```

At this point, the SSH key should be loaded on the Yubikey. Fetch the public key:

```bash
$ ssh-keygen -D $HOME/.nix-profile/lib/opensc-pkcs11.so -e
```

Note that the path to the `.so` file may differ, based on the setup. The above
is based on using Nix.

Add the key to the local `ssh-agent`:

```bash
$ _path=$(readlink $HOME/.nix-profile/lib/opensc-pkcs11.so) \
  ssh-add -s "$_path"
```

Note that the path must match the value that has been whitelisted on the
`ssh-agent` via the `-P` flag.

Confirm the key has been added:

```
$ ssh-add -L
```
