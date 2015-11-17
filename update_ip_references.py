#!/usr/bin/env python
'''
    update_ip_references.py
    Author: mstratton & npeterson
    Revised: 8/21/15
    ---------------------------------------------------------------------------
    This script grabs the IP address of the localhost and updates IP references
        in the specified text files

'''
import fileinput
import socket
import sys

# Get localhost IP address
ip_address = socket.gethostbyname(socket.gethostname())

# Define helper function for IP replacement
def update_ip(target_file, phrase):
    ''' Replace lines in file beginning with phrase, using new IP. '''
    updated_line = '{0}{1}\n'.format(phrase, ip_address)
    for line in fileinput.FileInput(target_file, inplace=1):
        if phrase in line:
            line = updated_line
        sys.stdout.write(line)
    return None

# Replace IP references in specified files
update_ip('cmap.properties', 'RunModel.MatrixServerAddress= ')
update_ip('cmap.properties', 'RunModel.HouseholdServerAddress= ')
update_ip('config/jppf-node0.properties', 'jppf.server.host = ')
update_ip('config/jppf-node1.properties', 'jppf.server.host = ')
update_ip('config/jppf-node2.properties', 'jppf.server.host = ')
update_ip('config/jppf-node3.properties', 'jppf.server.host = ')
#update_ip('config/jppf-node4.properties', 'jppf.server.host = ')
#update_ip('config/jppf-node5.properties', 'jppf.server.host = ')
#update_ip('config/jppf-node6.properties', 'jppf.server.host = ')
#update_ip('config/jppf-node7.properties', 'jppf.server.host = ')
#update_ip('config/jppf-node8.properties', 'jppf.server.host = ')
#update_ip('config/jppf-node9.properties', 'jppf.server.host = ')
update_ip('config/runMain.cmd', 'set HOST_IP=')
update_ip('config/runMain-SingleProcess.cmd', 'set HOST_IP=')
