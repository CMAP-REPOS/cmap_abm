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
    def _open_dbs(self):
        ''' Open base & test ABM database connections. '''
        self.base._open_db()
        self.test._open_db()
        return None


    def _close_dbs(self):
        ''' Close base & test ABM database connections. '''
        self.base._close_db()
        self.test._close_db()
        return None
