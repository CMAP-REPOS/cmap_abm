#!/usr/bin/env python
'''
    results_analysis.py
    Author: npeterson
    Revised: 3/15/16
    ---------------------------------------------------------------------------
    A module for reading TMM output files and matrix data into an SQL database
    for querying and summarization.

'''
from abm import *

# Set Base ABM parameters
BASE_DIR = r'X:\new_server_tests\test_5pct'
BASE_PCT = 0.05
BASE_BUILD = False

# Set Test ABM parameters
TEST_DIR = r'X:\new_server_tests\test_50pct_newsocec_3sp'
TEST_PCT = 0.5
TEST_BUILD = False

# Initialize Base ABM object & print summaries
print '\n{0:*^50}'.format(' P R O C E S S I N G ')
print '\n{0:=^50}\n'.format(' BASE NETWORK ')
base = ABM(BASE_DIR, BASE_PCT, BASE_BUILD)
base.open_db()
base.print_mode_share()
base.print_transit_stats()
base.print_ptrips_by_class()
base.print_vmt_by_speed()
base.close_db()
print base
print ' '

# Initialize Test ABM object & print summaries
print '\n{0:=^50}\n'.format(' TEST NETWORK ')
test = ABM(TEST_DIR, TEST_PCT, TEST_BUILD)
test.open_db()
test.print_mode_share()
test.print_transit_stats()
test.print_ptrips_by_class()
test.print_vmt_by_speed()
test.close_db()
print test
print ' '

# Initialize Test vs. Base Comparison object
print '\n{0:=^50}\n'.format(' COMPARISON ')
comp = Comparison(base, test)
print comp
print ' '

# Print comparison summaries
print '\n{0:*^50}'.format(' R E S U L T S ')
comp.open_dbs()
comp.print_mode_share_change()
comp.print_transit_stats_change()
comp.print_ptrips_by_class_change()
comp.print_vmt_by_speed_change()
comp.print_new_all()
comp.print_new_auto()
comp.print_new_dtt()
comp.print_new_wtt()
comp.print_new_other()
comp.close_dbs()
