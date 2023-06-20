#!/bin/bash

./aigtoaig "aigs/${1}.aag" "aigs/${1}.aig"
./abc -c "read aigs/${1}.aig; dc2; dc2; dc2; fraig; write_aiger benches/${1}.aig"
./aigtoaig "benches/${1}.aig" "benches/${1}.aag"
