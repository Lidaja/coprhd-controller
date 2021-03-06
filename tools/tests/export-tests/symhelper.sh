#!/bin/sh
#
# Copyright (c) 2015 EMC Corporation
# All Rights Reserved
#

# 
# Script to help manage storage system outside of ViPR.
# Used to perform various operations.
#
# Usage: ./symhelper.sh verify-export <SERIAL_NUMBER> <NAME_PATTERN> <NUMBER_OF_INITIATORS_EXPECTED> <NUMBER_OF_LUNS_EXPECTED>
#        ./symhelper.sh add_volume_to_mask <SERIAL_NUMBER> <DEVICE_ID> <NAME_PATTERN>
#        ./symhelper.sh remove_volume_from_mask <SERIAL_NUMBER> <DEVICE_ID> <NAME_PATTERN>
#        ./symhelper.sh delete_volume <SERIAL_NUMBER> <DEVICE_ID>
#        ./symhelper.sh add_initiator_to_mask <SERIAL_NUMBER> <PWWN> <NAME_PATTERN>
#        ./symhelper.sh remove_initiator_from_mask <SERIAL_NUMBER> <PWWN> <NAME_PATTERN>
#
#set -x

## Convenience method for deleting a mask outside of ViPR (including the storage group)
delete_mask() {
    serial_number=$1
    pattern=$2

    echo "y" | /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} delete view -name ${pattern}
    if [ $? -ne 0 ]; then
	echo "no mask found."
    fi

    /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -v | grep ${pattern}
    if [ $? -ne 0 ]; then
	echo "SG not found for ${pattern}"
    else
	sg_long_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -detail -v | grep ${pattern} | awk -F: '{print $2}' | awk '{print $1}' | sed -e 's/^[[:space:]]*//'`

        # Remove the volume from the storage group it is in
	dev_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type storage show ${sg_long_id} | grep Devices | awk -F: '{print $2}'`
	/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type storage -force -name ${sg_long_id} remove dev ${dev_id}
	
	# Put it back into the optimized SG?

	# Delete storage group
	/opt/emc/SYMCLI/bin/symaccess -sid 612 delete -force -name ${sg_long_id} -type storage
    fi
}

add_volume_to_mask() {
    serial_number=$1
    device_id=$2
    pattern=$3

    # Find out how many luns there are in the mask now.
    num_luns=`get_number_of_luns_in_mask ${serial_number} ${pattern}`

    # Find out where the volume ended up, thanks to ViPR (formalize this!)
    /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -dev ${device_id}
    if [ $? -ne 0 ]; then
	echo "Volume ${serial_number}_${device_id} was not found in another SG, skipping removal step"
    else
	sg_short_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -dev ${device_id} | grep ViPR_Optimized | cut -c1-31`
	sg_long_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -detail -v | grep ${sg_short_id} | awk -F: '{print $2}' | awk '{print $1}' | sed -e 's/^[[:space:]]*//'`

        # Remove the volume from the storage group it is in
	/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type storage -name $sg_long_id remove dev ${device_id}
    fi

    # Add it to the storage group ViPR knows about
    # TODO: I've seen storage groups sneak in over tests...make sure only storage group is found
    sg_short_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage | grep ${pattern}_SG | tail -1 | cut -c1-31`
    sg_long_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -detail -v | grep ${sg_short_id} | awk -F: '{print $2}' | awk '{print $1}' | sed -e 's/^[[:space:]]*//'`

    # Add the volume into the storage group we specify with the pattern
    /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type storage -name $sg_long_id add dev ${device_id}

    # Ensure the provider is updated
    verify_export_via_provider ${serial_number} ${pattern} none `expr ${num_luns} + 1`
}

remove_volume_from_mask() {
    serial_number=$1
    device_id=$2
    pattern=$3

    # Find out how many luns there are in the mask now.
    num_luns=`get_number_of_luns_in_mask ${serial_number} ${pattern}`

    # Add it to the storage group ViPR knows about
    sg_short_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage | grep ${pattern}_SG | tail -1 | cut -c1-31`
    sg_long_id=`/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type storage -detail -v | grep ${sg_short_id} | awk -F: '{print $2}' | awk '{print $1}' | sed -e 's/^[[:space:]]*//'`
    /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type storage -name $sg_long_id remove dev ${device_id}

    # Ensure the provider is updated
    verify_export_via_provider ${serial_number} ${pattern} none `expr ${num_luns} - 1`
}

add_initiator_to_mask() {
    serial_number=$1
    pwwn=$2
    pattern=$3

    # Find out how many inits there are in the mask now.
    num_inits=`get_number_of_initiators_in_mask ${serial_number} ${pattern}`

    # Find the initiator group that contains the pattern sent in
    /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type initiator | grep ${pattern}_IG
    if [ $? -ne 0 ]; then
	echo "Initiator group ${pattern}_IG was not found.  Not able to add to it."
    else
	# dd the initiator to the IG, which in turn adds it to the visibility of the mask
	/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type initiator -name ${pattern}_IG add -wwn ${pwwn}
    fi

    # Ensure the provider is updated
    verify_export_via_provider ${serial_number} ${pattern} `expr ${num_inits} + 1` none
}

remove_initiator_from_mask() {
    serial_number=$1
    pwwn=$2
    pattern=$3

    # Find out how many inits there are in the mask now.
    num_inits=`get_number_of_initiators_in_mask ${serial_number} ${pattern}`

    # Find the initiator group that contains the pattern sent in
    /opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} list -type initiator | grep ${pattern}_IG
    if [ $? -ne 0 ]; then
	echo "Initiator group ${pattern}_IG was not found.  Not able to add to it."
    else
	# dd the initiator to the IG, which in turn adds it to the visibility of the mask
	/opt/emc/SYMCLI/bin/symaccess -sid ${serial_number} -type initiator -name ${pattern}_IG remove -wwn ${pwwn}
    fi

    # Ensure the provider is updated
    verify_export_via_provider ${serial_number} ${pattern} `expr ${num_inits} - 1` none
}

delete_volume() {
    echo "Delete volume for VMAX not yet supported";
    sleep 30
}

verify_export_prechecks() {
    TMPFILE1=/tmp/verify-${RANDOM}
    TMPFILE2=/dev/null

    SYM=${SID:-000198700406}
    /opt/emc/SYMCLI/bin/symaccess -sid ${SYM} list view -name ${SG_PATTERN} -detail > ${TMPFILE1} 2> ${TMPFILE2}

    grep -n ${SG_PATTERN} ${TMPFILE1} > /dev/null
    if [ $? -ne 0 ]; then
	echo "ERROR: Expected MaskingView ${SG_PATTERN}, but could not find it";
	exit 1;
    fi
}

get_number_of_luns_in_mask() {
    # First parameter is the Symm ID
    SID=$1
    SG_PATTERN=$2
    NUM_INITIATORS=$3
    NUM_LUNS=$4

    verify_export_prechecks

    num_luns=`perl -nle 'print $1 if(m#(\S+)\s+\S+\s+Not Visible\s+#);' ${TMPFILE1} | sort -u | wc -l`
    echo ${num_luns}
    exit 0;
}

get_number_of_initiators_in_mask() {
    # First parameter is the Symm ID
    SID=$1
    SG_PATTERN=$2
    NUM_INITIATORS=$3
    NUM_LUNS=$4

    verify_export_prechecks

    num_inits=`grep "WWN.*:" ${TMPFILE1} | wc -l`
    echo ${num_inits}
    exit 0;
}

verify_export() {
    # First parameter is the Symm ID
    SID=$1
    shift
    # Subsequent parameters: MaskingView Name, Number of Initiators, Number of Luns
    # If checking if the MaskingView does not exist, then parameter $2 should be "gone"
    SG_PATTERN=$1
    NUM_INITIATORS=$2
    NUM_LUNS=$3
    TMPFILE1=/tmp/verify-${RANDOM}
    TMPFILE2=/dev/null

    SYM=${SID:-000198700406}
    /opt/emc/SYMCLI/bin/symaccess -sid ${SYM} list view -name ${SG_PATTERN} -detail > ${TMPFILE1} 2> ${TMPFILE2}

    grep -n ${SG_PATTERN} ${TMPFILE1} > /dev/null
    if [ $? -ne 0 ]
	then
	if [ "$2" = "gone" ]
	    then
	    echo "PASSED: Verified MaskingView with pattern ${SG_PATTERN} doesn't exist."
	    exit 0;
	fi
	echo "ERROR: Expected MaskingView ${SG_PATTERN}, but could not find it";
	exit 1;
    else
	if [ "$2" = "gone" ]
	    then
	    echo "ERROR: Expected MaskingView ${SG_PATTERN} to be gone, but it was found"
	    exit 1;
	fi
    fi

    num_inits=`grep "WWN.*:" ${TMPFILE1} | wc -l`
    num_luns=`perl -nle 'print $1 if(m#(\S+)\s+\S+\s+Not Visible\s+#);' ${TMPFILE1} | sort -u | wc -l`
    failed=false

    if [ "${num_inits}" != "${NUM_INITIATORS}" ]; then
	echo "FAILED: Export group initiators: Expected: ${NUM_INITIATORS}, Retrieved: ${num_inits}";
	echo "FAILED: Masking view dump:"
	grep "Masking View Name" ${TMPFILE1}
	grep "Group Name" ${TMPFILE1}
	grep "WWN.*:" ${TMPFILE1}
	grep "Not Visible" ${TMPFILE1} | sort -u
	failed=true
    fi

    if [ "${num_luns}" != "${NUM_LUNS}" ]; then
	echo "FAILED: Export group luns: Expected: ${NUM_LUNS}, Retrieved: ${num_luns}";
	echo "FAILED: Masking view dump:"
	grep "Masking View Name" ${TMPFILE1}
	grep "Group Name" ${TMPFILE1}
	grep "WWN" ${TMPFILE1}
	grep "Not Visible" ${TMPFILE1} | sort -u
	failed=true
    fi

    if [ "${failed}" = "true" ]; then
	exit 1;
    fi

    echo "PASSED: MaskingView '$1' contained $2 initiators and $3 luns"
    exit 0;
}


# This method will use the array tool to check the mask on the provider
verify_export_via_provider() {
    # First parameter is the Symm ID
    SID=$1
    MAX_WAIT_SECONDS=`expr 20 \* 60`
    SLEEP_INTERVAL_SECONDS=10
    shift
    # Subsequent parameters: MaskingView Name, Number of Initiators, Number of Luns
    # If checking if the MaskingView does not exist, then parameter $2 should be "gone"
    SG_PATTERN=$1
    NUM_INITIATORS=$2
    NUM_LUNS=$3
    TMPFILE1=/tmp/verify-${RANDOM}

    DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    tools_jar="${DIR}/preExistingConfig.jar"

    if [ ! -f preExistingConfig.properties ]; then
	echo "Missing preExistingConfg.properties.  dutests should generate this for you"
	exit 1
    fi
    
    if [ "none" != "${NUM_INITIATORS}" -a "none" != "${NUM_LUNS}" ]; then
	echo "Invalid parameters sent to verify_export_via_provider...."
	exit 1
    fi

    numinits="none"
    numluns="none"
    waited=0

    while [ "${numinits}" != "${NUM_INITIATORS}" -o "${numluns}" != "${NUM_LUNS}" ]
    do
      if [ "${numinits}" != "none" -o "${numluns}" != "none" ]; then
	  sleep ${SLEEP_INTERVAL_SECONDS}
	  waited=`expr ${waited} + ${SLEEP_INTERVAL_SECONDS}`

	  if [ ${waited} -ge ${MAX_WAIT_SECONDS} ]; then
	      echo "Waited, but never found provider to have the right number of luns and volumes"
	      exit 1;
	  fi
      fi

      # Gather results from the external tool
      java -Dlogback.configurationFile=./logback.xml -jar ${tools_jar} show-view $SID $SG_PATTERN $NUM_INITIATORS $NUM_LUNS > ${TMPFILE1}

      numinits=`grep -i "Total Number of Initiators for View" ${TMPFILE1} | awk '{print $NF}'`
      numluns=`grep -i "Total Number of Volumes For View" ${TMPFILE1} | awk '{print $NF}'`
      echo "Found ${numinits} initiators and ${numluns} volumes in mask ${SG_PATTERN}"

      if [ "${NUM_INITIATORS}" = "none" ]; then
	  numinits="none"
      fi

      if [ "${NUM_LUNS}" = "none" ]; then
	  numluns="none"
      fi
    done
}

create_export_mask() {
    SID=$1
    CSG=$2
    PWWN=$3
    NAME="${4}_${SID: -3}"

    IG=`echo "${NAME}_IG"`
    echo "Creating initiator group ${IG}"
    /opt/emc/SYMCLI/bin/symaccess -sid ${SID} create -type initiator -name ${IG} -wwn ${PWWN}

    PG=`echo "${CSG: 0:-4}_PG"`
    echo "Hijacking port group ${PG}"

    # Test if we were passed devIds or a CSG/SG name
    if [[ $CSG =~ [A-Za-z] ]]; then
        echo "Hijacking existing CSG/SG ${CSG}"
    else
        echo "Creating an SG with specified devs not yet supported"
    fi

    echo "Creating masking view ${NAME}"
    /opt/emc/SYMCLI/bin/symaccess -sid ${SID} create view -name ${NAME} -sg $CSG -pg ${PG} -ig ${IG}
}

delete_export_mask() {
    SID=$1
    NAME="${2}_${SID: -3}"
    IG=$3

    echo "Deleting masking view ${NAME}"
    /opt/emc/SYMCLI/bin/symaccess -sid ${SID} delete view -name ${NAME} -unmap -noprompt

    echo "Deleting initiator group ${IG}"
    /opt/emc/SYMCLI/bin/symaccess -sid ${SID} delete -type initiator -name ${IG} -force -noprompt
}

# Check to see if this is an operational request or a verification of export request
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
    delete_mask $1 $2
elif [ "$1" = "verify_export" ]; then
    shift
    verify_export $*
elif [ "$1" = "verify_export_via_provider" ]; then
    shift
    verify_export_via_provider $*
elif [ "$1" = "create_export_mask" ]; then
    shift
    create_export_mask $*
elif [ "$1" = "delete_export_mask" ]; then
    shift
    delete_export_mask $*
else
    # Backward compatibility with vmaxexport scripts
    verify_export $*
fi

