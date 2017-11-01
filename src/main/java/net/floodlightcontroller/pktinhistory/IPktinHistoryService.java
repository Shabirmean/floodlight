package net.floodlightcontroller.pktinhistory;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.SwitchMessagePair;
import net.floodlightcontroller.util.ConcurrentCircularBuffer;

/**
 * Created by shabirmean on 2017-10-31 with some hope.
 */
public interface IPktinHistoryService extends IFloodlightService {
    public ConcurrentCircularBuffer<SwitchMessagePair> getBuffer();
}
