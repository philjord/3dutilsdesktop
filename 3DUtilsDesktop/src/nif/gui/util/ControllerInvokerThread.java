package nif.gui.util;

import java.util.ArrayList;

import nif.j3d.animation.J3dNiControllerManager;
import nif.j3d.particles.tes3.J3dNiParticles;

public class ControllerInvokerThread extends Thread
{
	private J3dNiControllerManager cont;

	private J3dNiControllerManager optionalCont;

	// Either teh above OR the below!

	private ArrayList<J3dNiParticles> j3dNiParticless;

	public ControllerInvokerThread(String name, J3dNiControllerManager cont, J3dNiControllerManager optionalCont)
	{

		this.setDaemon(true);
		this.setName("ControllerInvokerThread " + name);

		this.cont = cont;
		this.optionalCont = optionalCont;
	}

	public ControllerInvokerThread(String name, ArrayList<J3dNiParticles> j3dNiParticless)
	{

		this.setDaemon(true);
		this.setName("ControllerInvokerThread " + name);

		this.j3dNiParticless = j3dNiParticless;

	}

	@Override
	public void run()
	{
		try
		{
			Thread.sleep(1000);
			if (j3dNiParticless != null)
			{
				boolean anyLive = true;
				while (anyLive)
				{
					Thread.sleep(((long) (Math.random() * 3000)));
					anyLive = false;

					long maxSleep = 0;
					for (J3dNiParticles j3dNiParticles : j3dNiParticless)
					{
						anyLive = anyLive || j3dNiParticles.isLive();
						System.out.println("firing " + j3dNiParticles);
						j3dNiParticles.fireSequence();

						maxSleep = j3dNiParticles.getLengthMS() > maxSleep ? j3dNiParticles.getLengthMS() : maxSleep;
					}

					Thread.sleep(maxSleep);
				}
				System.out.println("controller cleaning up");
			}
			else
			{
				String[] actions = cont.getAllSequences();
				while (cont.isLive())
				{
					for (int i = 0; i < actions.length; i++)
					{
						Thread.sleep((long) (Math.random() * 3000) + 1000);
						System.out.println("firing " + actions[i]);

						cont.getSequence(actions[i]).fireSequenceOnce();

						if (optionalCont != null)
							optionalCont.getSequence(actions[i]).fireSequenceOnce();

						Thread.sleep(cont.getSequence(actions[i]).getLengthMS());
					}
				}
			}
		}
		catch (InterruptedException e)
		{
		}
	}

}
