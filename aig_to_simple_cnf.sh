#!/bin/bash

./aigtoaig ksmt-temp/output.aag ksmt-temp/output.aig
./abc -c "read ksmt-temp/output.aig; dc2; dc2; dc2; fraig; write_cnf ksmt-temp/output.dimacs"
