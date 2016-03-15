#!/usr/bin/env python
'''
    to_csv.py
    Author: npeterson
    Revised: 3/15/16
    ---------------------------------------------------------------------------
    A set of functions for exporting summaries of ABM and Comparison objects
    to CSVs.

'''
import abm
import comparison

# -----------------------------------------------------------------------------
#  Define functions for exporting ABM summaries to CSVs.
# -----------------------------------------------------------------------------
def line_boardings(abm, csv_path):
    ''' Export a CSV file containing rail boardings by line, and total bus
        boardings. '''
    rail_codes = {
        'cbl': 'CTA Blue',
        'cbr': 'CTA Brown',
        'cga': 'CTA Green',  # Ashland/63rd
        'cgc': 'CTA Green',  # Cottage Grove
        'cor': 'CTA Orange',
        'cpk': 'CTA Pink',
        'cpr': 'CTA Purple',
        'crd': 'CTA Red',
        'cye': 'CTA Yellow',
        'mbn': 'Metra BNSF',
        'mhc': 'Metra HC',
        'mme': 'Metra ME',
        'mmn': 'Metra MD-N',
        'mmw': 'Metra MD-W',
        'mnc': 'Metra NCS',
        'mnw': 'Metra UP-NW',
        'mri': 'Metra RI',
        'mss': 'NICTD SS',
        'msw': 'Metra SWS',
        'mun': 'Metra UP-N',
        'muw': 'Metra UP-W'
    }

    # Get boardings by line ID, split into rail and bus
    boardings_dict = abm._get_boardings('LINE', split_rail=True, scale_runs=False)

    # Count total bus boardings
    bus_total = sum(b for b in boardings_dict['BUS'].itervalues())

    # Count rail boradings by line
    rail_by_line = {}
    for run, boardings in boardings_dict['RAIL'].iteritems():
        line_code = run[:3]
        line = rail_codes.get(line_code, line_code)  # Use line_code if not in rail_codes dict
        rail_by_line[line] = rail_by_line.get(line, 0) + boardings

    # Write results to CSV
    with open(csv_path, 'wb') as w:
        row = '{0},{1}\n'
        w.write(row.format('LINE', 'BOARDINGS'))
        w.write(row.format('All Buses', bus_total))
        for line, boardings in sorted(rail_by_line.iteritems()):
            w.write(row.format(line, boardings))

    return csv_path


def od_matrix(abm, csv_path):
    ''' Export a CSV containing two matrices of person-trip O-D zone
        groups: one for autos and one for transit. '''
    mode_groups = {
        1: 'AUTO',      # Drive alone free
        2: 'AUTO',      # Drive alone pay
        3: 'AUTO',      # Shared ride 2 free
        4: 'AUTO',      # Shared ride 2 pay
        5: 'AUTO',      # Shared ride 3+ free
        6: 'AUTO',      # Shared ride 3+ pay
        7: None,        # Walk
        8: None,        # Bike
        9: 'TRANSIT',   # Walk to local transit
        10: 'TRANSIT',  # Walk to premium transit
        11: 'TRANSIT',  # Drive to local transit
        12: 'TRANSIT',  # Drive to premium transit
        13: None,       # Taxi
        14: None,       # School bus
    }

    # Initialize results matrices
    zngrp_order = ['Chicago', 'Cook Balance', 'DuPage', 'Kane', 'Kendall', 'Lake', 'McHenry', 'Will', 'IL Balance', 'Indiana', 'Wisconsin']
    dim = len(zngrp_order)
    od_mat = {
        'AUTO': [[0 for i in xrange(dim)] for j in xrange(dim)],
        'TRANSIT': [[0 for i in xrange(dim)] for j in xrange(dim)]
    }

    # Append person-trip counts to appropriate matrix cell
    abm.open_db()
    trip_sql = (
        ''' SELECT Trips.zn_o, Trips.zn_d, Trips.mode, Tours.participants '''
        ''' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id '''
    )
    for zn_o, zn_d, mode, participants in abm.query(trip_sql):
        auto_or_transit = mode_groups[mode]
        if not auto_or_transit:
            continue  # Ignore non-motorized trips
        n = abm._unsample(len(participants.split()))
        for zngrp, zones in abm.zone_groups.iteritems():
            if zn_o in zones:
                zngrp_o = zngrp
                break
        for zngrp, zones in abm.zone_groups.iteritems():
            if zn_d in zones:
                zngrp_d = zngrp
                break
        i = zngrp_order.index(zngrp_o)
        j = zngrp_order.index(zngrp_d)
        od_mat[auto_or_transit][i][j] += n
    abm.close_db()

    # Write results to CSV
    with open(csv_path, 'wb') as w:
        for mode, matrix in sorted(od_mat.iteritems()):
            w.write(mode + ',' + ','.join(zngrp + ' (D)' for zngrp in zngrp_order) + '\n')
            for i, zngrp in enumerate(zngrp_order):
                w.write(zngrp + ' (O),' + ','.join(str(n) for n in matrix[i]) + '\n')
            w.write('\n')

    return csv_path


def trip_purpose(abm, csv_path):
    ''' Export a CSV file containing the number of tours made for each
        purpose and, within them, the purpose of the individual trips. '''
    abm.open_db()

    # Count person-tours by tour purpose
    tour_sql = ''' SELECT purpose, participants FROM Tours '''
    tour_counts = {}
    for tour_purpose, participants in abm.query(tour_sql):
        n = abm._unsample(len(participants.split()))
        tour_counts[tour_purpose] = tour_counts.get(tour_purpose, 0) + n

    # Count person-trips by tour purpose and trip destination purpose
    trip_counts = {tour_purpose: {} for tour_purpose in tour_counts.iterkeys()}
    trip_purposes = set()
    trip_sql = (
        ''' SELECT Tours.purpose, Tours.participants, Trips.purpose_d '''
        ''' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id '''
    )
    for tour_purpose, participants, trip_purpose in abm.query(trip_sql):
        trip_purposes.add(trip_purpose)
        n = abm._unsample(len(participants.split()))
        trip_counts[tour_purpose][trip_purpose] = trip_counts[tour_purpose].get(trip_purpose, 0) + n

    abm.close_db()

    # Write results to CSV
    with open(csv_path, 'wb') as w:
        w.write('PERSON-TOUR PURPOSE,PERSON-TRIP PURPOSE,COUNT\n')
        for tour_purpose in sorted(tour_counts):
            w.write('{0},,{1}\n'.format(tour_purpose.upper(), tour_counts[tour_purpose]))
            for trip_purpose in sorted(trip_purposes):
                w.write(',{0},{1}\n'.format(trip_purpose, trip_counts[tour_purpose].get(trip_purpose, 0)))

    return csv_path


def trip_tod(abm, csv_path):
    ''' Export a CSV file containing the number of person-trips in each
        time-of-day period, by broad trip purpose. '''
    abm.open_db()

    # Count person-trips by TOD and purpose
    trips = {i: {'H-W': 0, 'W-H': 0, 'H-O': 0, 'O-H': 0, 'NH': 0} for i in xrange(1,9)}
    trip_sql = (
        ''' SELECT Trips.purpose_o, Trips.purpose_d, Tours.participants, Trips.tod '''
        ''' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id ORDER BY Trips.trip_id '''
    )

    for purp_o, purp_d, participants, tod in abm.query(trip_sql):
        n = abm._unsample(len(participants.split()))
        if purp_d in ('work', 'school', 'university') and purp_o == 'home':
            purpose = 'H-W'
        elif purp_o in ('work', 'school', 'university') and purp_d == 'home':
            purpose = 'W-H'
        elif purp_o == 'home':
            purpose = 'H-O'
        elif purp_d == 'home':
            purpose = 'O-H'
        else:
            purpose = 'NH'
        trips[tod][purpose] += n

    abm.close_db()

    # Write results to CSV
    with open(csv_path, 'wb') as w:
        w.write('TIME-OF-DAY,HOME-WORK,WORK-HOME,HOME-OTHER,OTHER-HOME,NON-HOME-BASED\n')
        for tod, trip_dict in sorted(trips.iteritems()):
            w.write('{0},{1},{2},{3},{4},{5}\n'.format(tod, trip_dict['H-W'], trip_dict['W-H'], trip_dict['H-O'], trip_dict['O-H'], trip_dict['NH']))

    return csv_path


def vmt_statistics(abm, csv_path):
    ''' Export a CSV file containing VMT, stratified by zone group and
        facility type. '''
    # Initialize VMT dict
    vmt_subset = {g: {t: 0 for t in abm.facility_types} for g in abm.zone_groups}

    # Populate dict from Emme data
    emmebank = _eb.Emmebank(abm._emmebank_path)

    for tod in xrange(1, 9):

        # Auto VMT from links in TOD's highway network
        scenario_id_hwy = '{0}'.format(tod)
        scenario_hwy = emmebank.scenario(scenario_id_hwy)
        network_hwy = scenario_hwy.get_network()

        for link in network_hwy.links():
            # Stratify by zone group
            zn = link.i_node['@zone']
            for g, z in abm.zone_groups.iteritems():
                if zn in z:
                    zone_group = g
                    break
            # Stratify by facility type
            for t, v in abm.facility_types.iteritems():
                if link.volume_delay_func in v:
                    facility_type = t
                    break

            # Calculate VMT
            vol = (  # Convert vehicle-equivalents to vehicles
                link['@vso1n']/1 + link['@vso1t']/1 + link['@vho2n']/1 + link['@vho2t']/1 +
                link['@vho3n']/1 + link['@vho3t']/1 + link['@vltrn']/1 + link['@vltrt']/1 +
                link['@vmtrn']/2 + link['@vmtrt']/2 + link['@vhtrn']/3 + link['@vhtrt']/3
            )
            vmt = vol * link.length

            # Add VMT to appropriate group
            vmt_subset[zone_group][facility_type] += vmt

        # Bus VMT from transit segments in TOD's transit network
        scenario_id_trn = '10{0}'.format(tod)
        scenario_trn = emmebank.scenario(scenario_id_trn)
        network_trn = scenario_trn.get_network()

        for link in network_trn.links():
            # Stratify by zone group
            zn = link.i_node['@zone']
            for g, z in abm.zone_groups.iteritems():
                if zn in z:
                    zone_group = g
                    break
            # Stratify by facility type
            for t, v in abm.facility_types.iteritems():
                if link.volume_delay_func in v:
                    facility_type = v
                    break

            # Calculate headway- and TTF-adjusted VMT for each bus segment
            for tseg in link.segments():
                if tseg.line.mode in ('B', 'E', 'L', 'P', 'Q'):

                    # Calculate line-specific volume from headway: must be at least 1; ignore headways of 99 mins)
                    vol = max(abm.tod_minutes(tod) / tseg['@hdway'], 1) if tseg['@hdway'] != 99 else 1
                    vmt = vol * link.length

                    vmt_subset[zone_group][facility_type] += vmt

    emmebank.dispose()  # Close Emmebank, remove lock

    # Write results to CSV
    zngrp_order = ['Chicago', 'Cook Balance', 'DuPage', 'Kane', 'Kendall', 'Lake', 'McHenry', 'Will', 'IL Balance', 'Indiana', 'Wisconsin']
    factype_order = ['Expressway', 'Arterial', 'Ramp/Toll', 'Centroid']
    with open(csv_path, 'wb') as w:
        row = '{0},{1},{2}\n'
        w.write(row.format('DISTRICT', 'FACILITY_TYPE', 'VMT'))

        # Iterate through zone groups
        for zone_group in zngrp_order:
            for facility_type in factype_order:
                vmt = vmt_subset[zone_group][facility_type]
                w.write(row.format(zone_group, facility_type, vmt))
            vmt_subtotal = sum(v for v in vmt_subset[zone_group].itervalues())
            w.write(row.format(zone_group, 'Subtotal', vmt_subtotal))

        # Summarize entire network by facility type
        network_totals = {t: sum(vmt_subset[g][t] for g in abm.zone_groups) for t in abm.facility_types}
        for facility_type in factype_order:
            vmt = network_totals[facility_type]
            w.write(row.format('Entire Network', facility_type, vmt))
        grand_total = sum(v for v in network_totals.itervalues())
        w.write(row.format('Entire Network', 'Grand Total', grand_total))

        # Summarize CMAP 7-county region by facility type
        cmap_groups = ['Chicago', 'Cook Balance', 'McHenry', 'Lake', 'Kane', 'DuPage', 'Will', 'Kendall']
        cmap_totals = {t: sum(vmt_subset[g][t] for g in cmap_groups) for t in abm.facility_types}
        for facility_type in factype_order:
            vmt = network_totals[facility_type]
            w.write(row.format('CMAP Region', facility_type, vmt))
        cmap_total = sum(v for v in cmap_totals.itervalues())
        w.write(row.format('CMAP Region', 'Region Total', cmap_total))

    return csv_path


# -----------------------------------------------------------------------------
#  Define functions for exporting Comparison summaries to CSVs.
# -----------------------------------------------------------------------------
def persontrips_comparison(comparison, csv_path, geography='zone', trip_end='origin'):
    ''' Export a CSV file containing the mean base & test user classes for
        person-trips originating/ending in each zone/subzone. '''
    if trip_end not in ('origin', 'destination'):
        print 'CSV not exported: trip_end must be "origin" or "destination"!'
        return None
    elif geography == 'zone':
        group_field = 'Trips.zn_{0}'.format(trip_end[0])
    elif geography == 'subzone':
        group_field = 'Trips.sz_{0}'.format(trip_end[0])
    else:
        print 'CSV not exported: geography must be "zone" or "subzone"!'
        return None

    max_id = {
        'zone': 1944,
        'subzone': 16819
    }

    grouped_base_ptrips_by_class = comparison.base._get_ptrips_by_class(group_field)
    grouped_test_ptrips_by_class = comparison.test._get_ptrips_by_class(group_field)

    with open(csv_path, 'wb') as w:
        w.write('{0}_{1},ptrips_base,mean_uclass_base,ptrips_test,mean_uclass_test,ptrips_diff,mean_uclass_diff\n'.format(geography, trip_end[0]))
        for geog_id in xrange(1, max_id[geography]+1):
            where_filter = '{0} = {1}'.format(group_field, geog_id)

            if geog_id in grouped_base_ptrips_by_class:
                base_ptrips_by_class = grouped_base_ptrips_by_class[geog_id]
                base_ptrips_total = sum(base_ptrips_by_class.itervalues())
                if base_ptrips_total > 0:
                    mean_uclass_base = sum((uc * n for uc, n in base_ptrips_by_class.iteritems())) / base_ptrips_total
                else:
                    mean_uclass_base = 0
            else:
                mean_uclass_base = 0

            if geog_id in grouped_test_ptrips_by_class:
                test_ptrips_by_class = grouped_test_ptrips_by_class[geog_id]
                test_ptrips_total = sum(test_ptrips_by_class.itervalues())
                if test_ptrips_total > 0:
                    mean_uclass_test = sum((uc * n for uc, n in test_ptrips_by_class.iteritems())) / test_ptrips_total
                else:
                    mean_uclass_test = 0
            else:
                mean_uclass_test = 0

            ptrips_total_diff = test_ptrips_total - base_ptrips_total
            mean_uclass_diff = mean_uclass_test - mean_uclass_base

            row_template = '{0},{1:.0f},{2:.4f},{3:.0f},{4:.4f},{5:.0f},{6:.4f}\n'
            w.write(row_template.format(geog_id, base_ptrips_total, mean_uclass_base, test_ptrips_total, mean_uclass_test, ptrips_total_diff, mean_uclass_diff))

    print 'Person-trips and mean user class by {0} {1} have been exported to {2}.\n'.format(trip_end, geography, csv_path)
    return None
