object :- N : scene:<e,t>
object :- N/N : (lambda $0:<e,t> $0)
object that is :- N/N : (lambda $0:<e,t> $0)

other :- N/N : (lambda $0:<e,t> $0)

cylinders :- N : (lambda $0:<e,t> (filter_shape:<<e,t>,<psh,<e,t>>> $0 cylinder:psh))
other objects :- N : (lambda $0:<e,t> $0)

metallic :- N/N : (lambda $0:<e,t> (filter_material:<<e,t>,<pm,<e,t>>> $0 metal:pm))
shiny :- N/N : (lambda $0:<e,t> (filter_material:<<e,t>,<pm,<e,t>>> $0 metal:pm))
big :- N/N : (lambda $0:<e,t> (filter_size:<<e,t>,<psi,<e,t>>> $0 large:psi))
green :- N/N : (lambda $0:<e,t> (filter_color:<<e,t>,<pc,<e,t>>> $0 green:pc))
brown :- N/N : (lambda $0:<e,t> (filter_color:<<e,t>,<pc,<e,t>>> $0 brown:pc))
purple :- N/N : (lambda $0:<e,t> (filter_color:<<e,t>,<pc,<e,t>>> $0 purple:pc))

the :- S/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))
the :- N/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))

either :- S/N : (lambda $0:<e,t> $0)
or :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (union:<<e,t>,<<e,t>,<e,t>>> $0 $1)))
and :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (intersect:<<e,t>,<<e,t>,<e,t>>> $0 $1)))

other things that are the same shape as :- N/N : (lambda $0:e (same_shape:<e,<e,t>> $0))
//of the same shape as :- A/N : (lambda $0:e (same_shape:<e,<e,t>> $0))
the same size as :- N/N : (lambda $0:e (same_size:<e,<e,t>> $0))

are there any :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
is there a :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
what is the material of :- S/N : (lambda $0:e (query_material:<e,pm> $0))
what number of :- S/N/S : (lambda $0:<<e,t>,<e,t>> (lambda $1:<e,t> (count:<<e,t>,i> ($0 $1))))
are :- S/S : (lambda $0:<e,t> $0)
right of :- N/N : (lambda $0:e (relate:<e,<s,<e,t>>> $0 right:s))
left of :- N/N : (lambda $0:e (relate:<e,<s,<e,t>>> $0 left:s))
// are there more .. than ..
than :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (greater_than:<i,<i,t>> (count:<<e,t>,i> $1) (count:<<e,t>,i> $0)))
are there more :- S/N : (lambda $0:t $0)
