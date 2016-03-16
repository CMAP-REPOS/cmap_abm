#!/usr/bin/env python
'''
    results_analysis.py
    Author: npeterson
    Revised: 3/16/16
    ---------------------------------------------------------------------------
    A script for printing a series of summary statistics for base and test ABM
    objects and a Comparison object created from them.

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
print base
print_summary.mode_share(base)
print_summary.transit_stats(base)
print_summary.ptrips_by_class(base)
print_summary.vmt_by_speed(base)
print ' '


# Initialize Test ABM object & print summaries
print '\n{0:=^50}\n'.format(' TEST NETWORK ')
test = ABM(TEST_DIR, TEST_PCT, TEST_BUILD)
print test
print_summary.mode_share(test)
print_summary.transit_stats(test)
print_summary.ptrips_by_class(test)
print_summary.vmt_by_speed(test)
print ' '


# Initialize Test vs. Base Comparison object
print '\n{0:=^50}\n'.format(' COMPARISON ')
comp = Comparison(base, test)
print comp
print ' '


# Print Comparison summaries
print '\n{0:*^50}'.format(' R E S U L T S ')
print_summary.mode_share_change(comp)
print_summary.transit_stats_change(comp)
print_summary.ptrips_by_class_change(comp)
print_summary.vmt_by_speed_change(comp)
print_summary.new_all(comp)
print_summary.new_auto(comp)
print_summary.new_dtt(comp)
print_summary.new_wtt(comp)
print_summary.new_other(comp)
