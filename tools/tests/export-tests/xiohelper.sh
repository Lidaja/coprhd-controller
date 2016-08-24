#!/bin/sh
#
# Copyright (c) 2015 EMC Corporation
# All Rights Reserved
#

# 
# Script to help manage storage system outside of ViPR.
# Used to perform various operations.
#
# Usage: ./xiohelper.sh verify-export <NAME_PATTERN> <NUMBER_OF_INITIATORS_EXPECTED> <NUMBER_OF_LUNS_EXPECTED>
#        ./xiohelper.sh add_volume_to_mask <DEVICE_ID> <NAME_PATTERN>
#        ./xiohelper.sh remove_volume_from_mask <DEVICE_ID> <NAME_PATTERN>
#        ./xiohelper.sh delete_volume <DEVICE_ID>
#        ./xiohelper.sh delete_mask <NAME_PAATTERN>
#        ./xiohelper.sh add_initiator_to_mask <PWWN> <NAME_PATTERN>
#        ./xiohelper.sh remove_initiator_from_mask <PWWN> <NAME_PATTERN>
#
#set -x

## Convenience method for deleting a mask outside of ViPR (including the storage group)
delete_mask() {
    pattern=$1
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method delete_mask -params "${pattern}" 
    echo "Deleted lun mapping/initiator group ${pattern}"
}

add_volume_to_mask() {
    device_id=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method add_volume_to_mask -params "${device_id},${pattern}" 
    echo "Added volume ${device_id} to initiator group ${pattern}"
}

remove_volume_from_mask() {
    device_id=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method remove_volume_from_mask -params "${device_id},${pattern}"
    echo "Removed volume ${device_id} from initiator group ${pattern}"
}

delete_volume() {
    device_id=$1
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method delete_volume -params "${device_id}" 
}

remove_initiator_from_mask() {
    pwwn=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method remove_initiator_from_mask -params "${pwwn},${pattern}"

}

add_initiator_to_mask() {
    pwwn=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method add_initiator_to_mask -params "${pwwn},${pattern}"

}

verify_export() {
    # Parameters: Initiator group Name, Number of Initiators, Number of Luns
    # If checking if the Initiator group does not exist, then parameter $2 should be "gone"
    IG_PATTERN=$1
    NUM_INITIATORS=$2
    NUM_LUNS=$3
    TMPFILE1=/tmp/verify-${RANDOM}
    TMPFILE2=/dev/null

    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays xtremio -method get_initiator_group -params ${IG_PATTERN} > ${TMPFILE1} 2> ${TMPFILE2}
    grep -n ${IG_PATTERN} ${TMPFILE1} > /dev/null
    if [ $? -ne 0 ]
	then
	if [ "$2" = "gone" ]
	    then
	    echo "PASSED: Verified MaskingView with pattern ${IG_PATTERN} doesn't exist."
	    exit 0;
	fi
	echo "ERROR: Expected MaskingView ${IG_PATTERN}, but could not find it";
	exit 1;
    else
	if [ "$2" = "gone" ]
	    then
	    echo "ERROR: Expected MaskingView ${IG_PATTERN} to be gone, but it was found"
	    exit 1;
	fi
    fi

    num_inits=`grep -Po '(?<="numberOfInitiators":")[^"]*' ${TMPFILE1}`
    num_luns=`grep -Po '(?<="numberOfVolumes":")[^"]*' ${TMPFILE1}`
    failed=false

    if [ ${num_inits} -ne ${NUM_INITIATORS} ]
	then
	echo "FAILED: Export group initiators: Expected: ${NUM_INITIATORS}, Retrieved: ${num_inits}";
	failed=true
    fi

    if [ ${num_luns} -ne ${NUM_LUNS} ]
	then
	echo "FAILED: Export group luns: Expected: ${NUM_LUNS}, Retrieved: ${num_luns}";
	failed=true
    fi

    if [ "${failed}" = "true" ]
	then
	exit 1;
    fi

    echo "PASSED: Initiator group '$1' contained $2 initiators and $3 luns"
    exit 0;
}

# Check to see if this is an operational request or a verification of export request
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
tools_file="${DIR}/tools.yml"
tools_jar="${DIR}/ArrayTools.jar"
if [ "$1" = "add_volume_to_mask" ]; then
    shift
    add_volume_to_mask $1 $2 $3
elif [ "$1" = "remove_volume_from_mask" ]; then
    shift
    remove_volume_from_mask $1 $2 $3
elif [ "$1" = "add_initiator_to_mask" ]; then
    shift
    add_initiator_to_mask $1 $2 $3
elif [ "$1" = "remove_initiator_from_mask" ]; then
    shift
    remove_initiator_from_mask $1 $2 $3
elif [ "$1" = "delete_volume" ]; then
    shift
    delete_volume $1 $2
elif [ "$1" = "delete_mask" ]; then
    shift
    delete_mask $1
elif [ "$1" = "verify_export" ]; then
    shift
    verify_export $*
else
    echo "Usage: $0 [delete_mask | add_volume_to_mask | remove_volume_from_mask | add_initiator_to_mask | remove_initiator_from_mask | delete_volume | verify_export] {params}"
fi