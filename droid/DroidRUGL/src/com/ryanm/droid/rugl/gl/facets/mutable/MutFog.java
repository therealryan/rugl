package com.ryanm.droid.rugl.gl.facets.mutable;

import com.ryanm.droid.rugl.gl.enums.FogMode;
import com.ryanm.droid.rugl.gl.facets.Fog;
import com.ryanm.droid.rugl.util.Colour;
import com.ryanm.preflect.annote.Variable;
import com.ryanm.preflect.annote.WidgetHint;

/**
 * Controls fog
 * 
 * @author ryanm
 */
@Variable( "Fog" )
public class MutFog extends MutableFacet<Fog>
{
	/***/
	@Variable
	public boolean enabled = false;

	/***/
	@Variable
	public FogMode mode = FogMode.EXP;

	/***/
	@Variable
	public float density = 1;

	/***/
	@Variable
	public float start = 0;

	/***/
	@Variable
	public float end = 1;

	/***/
	@Variable
	@WidgetHint( Colour.class )
	public int colour = 0;

	/**
	 * @param f
	 */
	public MutFog( final Fog f )
	{
		enabled = f.enabled;
		mode = f.mode;
		density = f.density;
		start = f.start;
		end = f.end;
		colour = f.colour;
	}

	@Override
	public Fog compile()
	{
		if( enabled )
		{
			return new Fog( mode, density, start, end, colour );
		}
		else
		{
			return Fog.disabled;
		}
	}
}
