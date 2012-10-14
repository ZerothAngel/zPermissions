#!/usr/bin/env python

import random


WORLDS = [None] * 8 + ['world', 'world_nether', 'creative', 'hardcore']

REGIONS = [None] * 20 + ['Region%d' % i for i in range(10)]

NUM_PLAYERS = 100

NUM_PERMISSIONS_PER_PLAYER = 50

NUM_GROUPS = (3, 13, 23, 31, 41)

NUM_PERMISSIONS_PER_GROUP = 50

NUM_PLAYERS_PER_GROUP = 50

PLAYER_MEMBER_POOL_SIZE = 1000


groups_at_depth = []
for i in range(len(NUM_GROUPS)):
    groups_at_depth.append([])


def generate_permissions(name, is_group, count):
    for i in range(count):
        region = REGIONS[random.randint(0, len(REGIONS) - 1)]
        if region is None:
            region = ''
        else:
            region += '/'
        world = WORLDS[random.randint(0, len(WORLDS) - 1)]
        if world is None:
            world = ''
        else:
            world += ':'
        print('permissions %s %s set %s%spermission.%s.%d true' % (
            is_group and 'group' or 'player',
            name,
            region,
            world,
            name,
            i))


def generate_group(name, depth):
    if depth == 0:
        # Nothing special
        print('permissions group %s create' % name)
    else:
        print('permissions group %s create' % name)
        # Pick random parent of previous depth
        potential_parents = groups_at_depth[depth - 1]
        parent = potential_parents[random.randint(0, len(potential_parents) - 1)]
        print('permissions group %s setparent %s' % (name, parent))
    assert name not in groups_at_depth[depth]
    groups_at_depth[depth].append(name)


def generate_members(name, count):
    for i in range(count):
        p = random.randint(0, PLAYER_MEMBER_POOL_SIZE - 1)
        print('permissions group %s add TestPlayer%d' % (name, p))


def main():
    for p in range(NUM_PLAYERS):
        generate_permissions('TestPlayer%d' % p, False,
                             NUM_PERMISSIONS_PER_PLAYER)

    group_count = 0
    for depth, num_at_depth in enumerate(NUM_GROUPS):
        for g in range(num_at_depth):
            name = 'TestGroup%d' % group_count
            group_count += 1
            generate_group(name, depth)
            generate_permissions(name, True, NUM_PERMISSIONS_PER_GROUP)
            generate_members(name, NUM_PLAYERS_PER_GROUP)


if __name__ == '__main__':
    main()
