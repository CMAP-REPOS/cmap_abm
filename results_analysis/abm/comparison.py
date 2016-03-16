#!/usr/bin/env python
'''
    comparison.py
    Author: npeterson
    Revised: 3/16/16
    ---------------------------------------------------------------------------
    A class for comparing the contents of two ABM object databases.

'''
import copy
import abm


class Comparison(object):
    ''' A class for comparing two ABM objects. Initialized with a base and a
        test ABM object. All comparisons are relative to the base. '''

    # --- Init ---
    def __init__(self, base_abm, test_abm):
        self.base = base_abm
        self.test = test_abm
        return None


    def __str__(self):
        return '[Comparison: BASE {0}; TEST {1}]'.format(self.base, self.test)


    # --- Instance methods ---
    def open_dbs(self):
        ''' Open base & test ABM database connections. '''
        self.base.open_db()
        self.test.open_db()
        return None


    def close_dbs(self):
        ''' Close base & test ABM database connections. '''
        self.base.close_db()
        self.test.close_db()
        return None
