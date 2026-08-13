# Threat model and unsupported environments

Status: `SCAFFOLD`. R000 records the frame; the populated model is owned by a
later phase.

## R000 attack surface

The R000 artifact is an Android shell that renders static build identity. It
requests no permissions, opens no network connection, reads no sensor, and
writes no persistent state. Its attack surface is the Android package itself.

## Unsupported environments

Not yet enumerated. Device and OS support boundaries are recorded once the
device matrix in `qualification/device-matrix/` exists.

## Data handling in R000

No personal data, sensor data, credential or secret is collected, stored or
transmitted by the R000 artifact or by this repository.
