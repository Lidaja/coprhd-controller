#!/bin/sh
#
# Copyright (c) 2016 EMC Corporation
# All Rights Reserved
#

# 
# Script to help manage storage system outside of ViPR.
# Used to perform various operations.
#
# Usage: ./vplexhelper.sh verify-export <NAME_PATTERN> <NUMBER_OF_INITIATORS_EXPECTED> <NUMBER_OF_LUNS_EXPECTED>
#        ./vplexhelper.sh add_volume_to_mask <DEVICE_ID> <NAME_PATTERN>
#        ./vplexhelper.sh remove_volume_from_mask <DEVICE_ID> <NAME_PATTERN>
#        ./vplexhelper.sh delete_volume <DEVICE_ID>
#        ./vplexhelper.sh add_initiator_to_mask <PWWN> <NAME_PATTERN>
#        ./vplexhelper.sh remove_initiator_from_mask <PWWN> <NAME_PATTERN>
#
#set -x

TMPFILE1=/tmp/verify-${RANDOM}
TMPFILE2=$TMPFILE1-error

add_volume_to_mask() {
    device_id=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays vplex -method add_volume_to_mask -params "${device_id},${pattern}" > ${TMPFILE1} 2> ${TMPFILE2}
}

remove_volume_from_mask() {
    device_id=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays vplex -method remove_volume_from_mask -params "${device_id},${pattern}" > ${TMPFILE1} 2> ${TMPFILE2}
}

delete_volume() {
    device_id=$1
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays vplex -method delete_volume -params "${device_id}" > ${TMPFILE1} 2> ${TMPFILE2}
}

remove_initiator_from_mask() {
    pwwn=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays vplex -method remove_initiator_from_mask -params "${pwwn},$(echo $pwwn | sed 's/^1/2/g'),${pattern}" > ${TMPFILE1} 2> ${TMPFILE2}
}

add_initiator_to_mask() {
    pwwn=$1
    pattern=$2
    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays vplex -method add_initiator_to_mask -params "${pwwn},$(echo $pwwn | sed 's/^1/2/g'),${pattern}" > ${TMPFILE1} 2> ${TMPFILE2}
}

verify_export() {
    # Parameters: Storage View Name Name, Number of Initiators, Number of Luns
    # If checking if the Storage View does not exist, then parameter $2 should be "gone"
    STORAGE_VIEW_NAME=$1
    NUM_INITIATORS=$2
    NUM_LUNS=$3

    java -Dproperty.file=${tools_file} -jar ${tools_jar} -arrays vplex -method get_storage_view -params ${STORAGE_VIEW_NAME} > ${TMPFILE1} 2> ${TMPFILE2}
    grep -n ${STORAGE_VIEW_NAME} ${TMPFILE1} > /dev/null
    # 0 if line selected, 1 if no line selected
    foundIt=$?
    if [ -s $TMPFILE1 ]; then
        if [ $foundIt -ne 0 ]; then
            if [ "$2" = "gone" ]; then
                echo "PASSED: Verified Storage View with pattern ${STORAGE_VIEW_NAME} doesn't exist."
                exit 0;
            fi
            echo "ERROR: I Expected Storage View ${STORAGE_VIEW_NAME}, but could not find it";
            exit 1;
        else
            if [ "$2" = "gone" ]; then
                echo "ERROR: Expected Storage View ${STORAGE_VIEW_NAME} to be gone, but it was found"
                exit 1;
            fi
        fi
    else
        echo "ERROR: empty or invalid response from vplex"
        exit 1;
    fi

    num_inits=`grep numberOfInitiators ${TMPFILE1} | awk -F: '{print $2}'`
    num_luns=`grep numberOfVolumes ${TMPFILE1} | awk -F: '{print $2}'`
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
elif [ "$1" = "verify_export" ]; then
    shift
    verify_export $*
else
    echo "Usage: $0 [add_volume_to_mask | remove_volume_from_mask | add_initiator_to_mask | remove_initiator_from_mask | delete_volume | verify_export] {params}"
fi
