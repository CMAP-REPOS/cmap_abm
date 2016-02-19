from results import *

GROUPS = 6
FRACTION = 0.5
REBUILD_DBS = False

# Initialize ABM objects
b = ABM(r'X:\CMAQ_ABM_Models\cmaq_base_20141204', 0.20, REBUILD_DBS)

t_ln1 = ABM(r'X:\CMAQ_ABM_Models\cmaq_line_min_20150105', 0.20, REBUILD_DBS)
t_ln2 = ABM(r'X:\CMAQ_ABM_Models\cmaq_line_mod_20150112', 0.20, REBUILD_DBS)
t_ln3 = ABM(r'X:\CMAQ_ABM_Models\cmaq_line_max_20141208', 0.20, REBUILD_DBS)

t_nd1 = ABM(r'X:\CMAQ_ABM_Models\cmaq_node_min_20141222', 0.20, REBUILD_DBS)
t_nd2 = ABM(r'X:\CMAQ_ABM_Models\cmaq_node_mod_20150109', 0.20, REBUILD_DBS)
t_nd3 = ABM(r'X:\CMAQ_ABM_Models\cmaq_node_max_20141215', 0.20, REBUILD_DBS)


# Get base boardings and groups
boardings_b = {
    'LINE': b._get_boardings('LINE'),
    'NODE': b._get_boardings('NODE')
}

groups_b = {
    'LINE': b._get_boarding_groups(boardings_b['LINE'], GROUPS, FRACTION),
    'NODE': b._get_boarding_groups(boardings_b['NODE'], GROUPS, FRACTION)
}


# Print base boardings
print '\n', b

for node_or_line in sorted(groups_b.keys()):
    print node_or_line, 'BASE MEAN BOARDINGS'
    grp_b = groups_b[node_or_line]
    brd_b = boardings_b[node_or_line]

    # Print mean base boardings by quantile
    for i in xrange(GROUPS):
        n = sum((1 for k in grp_b.iterkeys() if grp_b[k] == i+1))
        mean_base_brd = sum((brd_b[k] for k in grp_b.iterkeys() if grp_b[k] == i+1)) / n if n else 0.
        print 'G{0} ({1}): {2}'.format(i+1, n, mean_base_brd)

    # Print mean base boardings for all nodes/lines
    n = sum((1 for k in grp_b.iterkeys()))
    mean_base_brd = sum((brd_b[k] for k in grp_b.iterkeys())) / n if n else 0.  # Mean of all boardings
    print 'ALL ({0}): {1}\n'.format(n, mean_base_brd)


# Print groups' modal composition
for node_or_line in ['LINE']:
    print node_or_line, 'MODAL COMPOSITION'
    grp_b = groups_b[node_or_line]
    brd_b = boardings_b[node_or_line]

    # Print mean base boardings by quantile
    for i in xrange(GROUPS):
        modes = set((str(tline[0]).upper() for tline in grp_b.iterkeys()))
        n = {}
        for mode in sorted(modes):
            n[mode] = sum((1. for k in grp_b.iterkeys() if grp_b[k] == i+1 and str(k[0]).upper() == mode))
        pct = {mode: round(100.0 * n[mode] / sum(n.itervalues()), 2) for mode in n.keys()}
        print 'G{0}: {1}'.format(i+1, pct)

print '\n'


# Compare test scenario boardings with base, by quantile
def compare_boardings(t, node_or_line, mode=''):
    print '\n', t, mode

    if mode:
        grp_b = groups_b[node_or_line][mode]
        brd_b = boardings_b[node_or_line][mode]
        brd_t = t._get_boardings(node_or_line, split_rail=True)[mode]
    else:
        grp_b = groups_b[node_or_line]
        brd_b = boardings_b[node_or_line]
        brd_t = t._get_boardings(node_or_line, split_rail=False)

    brd_diff = {k: brd_t[k] - brd_b[k] for k in brd_b.iterkeys()}

    # Print mean additional boardings by quantile
    for i in xrange(GROUPS):
        n = sum((1 for k in grp_b.keys() if grp_b[k] == i+1))
        mean_new_brd = sum((brd_diff[k] for k in grp_b.iterkeys() if grp_b[k] == i+1)) / n if n else 0.
        print 'G{0} ({1}): {2}'.format(i+1, n, mean_new_brd)

    # Print mean additional boardings for all nodes/lines
    n = sum((1 for k in grp_b.keys()))
    mean_new_brd = sum((brd_diff[k] for k in grp_b.iterkeys())) / n if n else 0.  # Mean of all boardings
    print 'ALL ({0}): {1}\n'.format(n, mean_new_brd)

    return brd_t

boardings_t_ln1 = compare_boardings(t_ln1, 'LINE')
boardings_t_ln2 = compare_boardings(t_ln2, 'LINE')
boardings_t_ln3 = compare_boardings(t_ln3, 'LINE')

boardings_t_nd1 = compare_boardings(t_nd1, 'NODE')
boardings_t_nd2 = compare_boardings(t_nd2, 'NODE')
boardings_t_nd3 = compare_boardings(t_nd3, 'NODE')
