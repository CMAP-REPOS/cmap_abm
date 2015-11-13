#!/usr/bin/env python
'''
    set_model_paths.py
    Author: npeterson
    Revised: 4/30/15
    ---------------------------------------------------------------------------
    Replace "basic_template" fake-paths with working paths to current model.

'''
import os

# Define template string and output string to replace it with
template_string = '{{{TEMPLATE}}}'
output_string = os.path.basename(os.path.dirname(os.getcwd()))  # Name of parent-dir to "model"

# Define files to make the replacement in, relative to this script's location
filenames = [
    ['cmap.properties'],
    ['runCTRAMP.bat'],
    ['runCTRAMP-SingleProcess.bat'],
    ['runModel.bat'],
    ['config', 'runMain.cmd'],
    ['config', 'runMain-SingleProcess.cmd'],
    ['config', 'runNode0.cmd'],
    ['config', 'runNode1.cmd'],
    ['config', 'runNode2.cmd'],
    ['config', 'runNode3.cmd'],
    #['config', 'runNode4.cmd'],
    #['config', 'runNode5.cmd'],
    #['config', 'runNode6.cmd'],
    #['config', 'runNode7.cmd'],
    #['config', 'runNode8.cmd'],
    #['config', 'runNode9.cmd'],
]

# Iterate through files and perform find-and-replace
def find_replace(infile, find, replace):
    ''' Helper function to perform find-and-replace on a single file. '''
    with open(infile) as f:
        s = f.read()
    s = s.replace(find, replace)
    with open(infile, 'w') as f:
        f.write(s)

for filename in filenames:
    filepath = os.path.join(os.getcwd(), *filename)
    find_replace(filepath, template_string, output_string)
    print filepath
