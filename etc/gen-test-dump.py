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

METADATA_TYPES = [str, int, float, bool]

STR_ALPHABET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'

NUM_METADATA_PER_PLAYER = 10

NUM_METADATA_PER_GROUP = 10


groups_at_depth = []
for i in range(len(NUM_GROUPS)):
    groups_at_depth.append([])


def generate_metadata(name, is_group, count):
    for i in range(count):
        type = METADATA_TYPES[random.randint(0, len(METADATA_TYPES) - 1)]
        suffix = ''
        if type is str:
            length = random.randint(5,20)
            value = []
            for j in range(length):
                value.append(STR_ALPHABET[random.randint(0, len(STR_ALPHABET) - 1)])
            value = ''.join(value)
        elif type is int:
            suffix = 'int'
            value = random.randint(-10000, 10000)
        elif type is float:
            suffix = 'real'
            value = random.random() * 20000.0 - 10000.0
        elif type is bool:
            suffix = 'bool'
            value = random.randint(0,1)
            if value == 0:
                value = 'false'
            else:
                value = 'true'
        print('permissions %s %s metadata set%s metadata.%s.%d %s' % (
            is_group and 'group' or 'player',
            name,
            suffix,
            name.lower(),
            i,
            value))


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
            region.lower(),
            world.lower(),
            name.lower(),
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
        print('permissions group %s setparents %s' % (name, parent))
    print('permissions group %s setweight 0' % name)
    assert name not in groups_at_depth[depth]
    groups_at_depth[depth].append(name)


def generate_members(name, count):
    members = set([])
    while len(members) < count:
        p = random.randint(0, PLAYER_MEMBER_POOL_SIZE - 1)
        if p in members:
            continue
        members.add(p)
        print('permissions group %s add testplayer%d' % (name, p))


def main():
    for p in range(NUM_PLAYERS):
        generate_permissions('TestPlayer%d' % p, False,
                             NUM_PERMISSIONS_PER_PLAYER)
        generate_metadata('TestPlayer%d' % p, False, NUM_METADATA_PER_PLAYER)

    group_count = 0
    for depth, num_at_depth in enumerate(NUM_GROUPS):
        for g in range(num_at_depth):
            name = 'TestGroup%d' % group_count
            group_count += 1
            generate_group(name, depth)
            generate_permissions(name, True, NUM_PERMISSIONS_PER_GROUP)
            generate_members(name, NUM_PLAYERS_PER_GROUP)
            generate_metadata(name, True, NUM_METADATA_PER_GROUP)


if __name__ == '__main__':
    main()
