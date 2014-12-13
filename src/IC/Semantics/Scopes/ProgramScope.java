package IC.Semantics.Scopes;

import java.util.Comparator;


public class ProgramScope extends Scope {

	public ProgramScope(String id) {
		super(id, null);
		final Comparator<String> originalComparator = getComparator();
		setComparator(new Comparator<String>() {
			
			@Override
			public int compare(String o1, String o2) {

				if (o1.equals("Library"))
					return -1;
				if (o2.equals("Library"))
					return 1;
				
				return originalComparator.compare(o1, o2);
			}
		});
	}
	
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

}
