/*
 * Copyright (c) 2008-2011 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.plugins.common.commandgenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.cim.CIMArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All the necessary components needed to execute this command object are
 * encapsulated within the command Object itself. It exposes an Execute method,
 * which can be used by clients to invoke the command Object. Clients like
 * SMIPlugin, VNXFilePlugin,IsilonPlugin etc.
 */
public final class CommandImpl implements Command {
    /**
     * Instance reference on which method needs to get executed.
     */
    private Object _instance;
    /**
     * Method to execute.
     */
    private Method _method;
    /**
     * Input Argument Array.
     */
    private Object[] _inputArgs;

    private int _commandIndex;
    /**
     * Logger.
     */
    private static final Logger _logger = LoggerFactory.getLogger(CommandImpl.class);

    /**
     * Execute.
     * 
     * @throws IllegalArgumentException
     *             ex.
     * @throws IllegalAccessException
     *             ex.
     * @throws InvocationTargetException
     *             ex.
     * @return Object result.
     */
    @Override
    public Object execute() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        Object result = null;
        Object outputarg = _inputArgs[_inputArgs.length - 1];
        boolean isInvoke = isMethodInvoke(outputarg);
        _logger.debug("Is Method invoke :" + isInvoke);
        try {
            result = _method.invoke(_instance, _inputArgs);
            if (isInvoke) {
                result = outputarg;
            }
        } catch (Exception e) {
            _logger.error("Method invoke command execution failed :" + e.getLocalizedMessage());
        } finally {
            _instance = null;
        }
        return result;
    }

    /**
     * set the Method to execute.
     * 
     * @param method
     *            .
     */
    protected void setMethod(final Method method) {
        _method = method;
    }

    /**
     * set the input Argument Array.
     * 
     * @param valuesForInputArgs
     *            args.
     */
    protected void setInputArgs(final Object[] valuesForInputArgs) {
        _inputArgs = (null == valuesForInputArgs ? new Object[0] : valuesForInputArgs
                .clone());
    }

    /**
     * set Instance.
     * 
     * @param instance
     *            Instance.
     */
    protected void setInstance(final Object instance) {
        _instance = instance;
    }

    /**
     * get Instance
     * 
     * @return Object.
     */
    public Object getInstance() {
        return _instance;
    }

    /**
     * Check whether the method is of type INVOKE
     * 
     * @param outputarg
     * @return boolean
     */
    private final boolean isMethodInvoke(Object outputarg) {
        boolean isInvoke = false;
        if (outputarg instanceof CIMArgument<?>[]) {
            isInvoke = true;
        }
        return isInvoke;
    }

    /**
     * for debug purpose
     */
    public final Object[] retreiveArguments() {
        return _inputArgs.clone();
    }

    public void setCommandIndex(int commandIndex) {
        _commandIndex = commandIndex;
    }

    public int getCommandIndex() {
        return _commandIndex;
    }
}
